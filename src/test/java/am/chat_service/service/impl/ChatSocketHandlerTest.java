package am.chat_service.service.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatOpenedAckRequest;
import am.chat_service.dto.request.MessagesReadRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.event.ChatEvent;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.SocketConnectionException;
import am.chat_service.model.enums.EventType;
import am.chat_service.model.enums.MessageStatus;
import am.chat_service.model.enums.SocketEvent;
import am.chat_service.service.ChatMemberService;
import am.chat_service.service.ChatMessageService;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatSocketHandler}.
 *
 * <p>Inbound handlers are exercised by capturing the {@link DataListener}s the
 * handler registers on the {@link SocketIOServer} in {@code registerEventListeners}
 * and invoking them directly. Outbound broadcasting is tested through the public
 * {@code handleChatEvent} dispatch and {@code sendToUser}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatSocketHandlerTest {

    @Mock
    private SocketIOServer server;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatEventPublisher eventPublisher;
    @Mock
    private ChatMemberService chatMemberService;

    @InjectMocks
    private ChatSocketHandler handler;

    private ConnectListener connectListener;
    private final Map<String, DataListener<?>> dataListeners = new HashMap<>();

    @BeforeEach
    void registerAndCaptureListeners() {
        handler.registerEventListeners();

        ArgumentCaptor<ConnectListener> connectCaptor = ArgumentCaptor.forClass(ConnectListener.class);
        verify(server).addConnectListener(connectCaptor.capture());
        connectListener = connectCaptor.getValue();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DataListener> listenerCaptor = ArgumentCaptor.forClass(DataListener.class);
        verify(server, times(8)).addEventListener(nameCaptor.capture(), any(), listenerCaptor.capture());

        List<String> names = nameCaptor.getAllValues();
        List<DataListener> listeners = listenerCaptor.getAllValues();
        for (int i = 0; i < names.size(); i++) {
            dataListeners.put(names.get(i), listeners.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> DataListener<T> listenerFor(SocketEvent event) {
        return (DataListener<T>) dataListeners.get(event.value());
    }

    private SocketIOClient clientWithUser(long userId, UUID sessionId) {
        SocketIOClient client = mock(SocketIOClient.class);
        HandshakeData handshake = mock(HandshakeData.class);
        when(client.getHandshakeData()).thenReturn(handshake);
        when(handshake.getSingleUrlParam("userId")).thenReturn(String.valueOf(userId));
        when(client.getSessionId()).thenReturn(sessionId);
        return client;
    }

    private ChatMessageDto messageFrom(long senderUserId) {
        return new ChatMessageDto(100L, senderUserId, "hello", MessageStatus.SENT, null, null);
    }

    // ---- onConnect / onDisconnect -----------------------------------------

    @Test
    void onConnect_registersUserSoLaterSendReachesTheClient() {
        UUID session = UUID.randomUUID();
        SocketIOClient client = clientWithUser(7L, session);
        when(server.getClient(session)).thenReturn(client);

        connectListener.onConnect(client);
        handler.sendToUser(7L, "ping", Map.of("k", "v"));

        verify(client).sendEvent("ping", Map.of("k", "v"));
    }

    @Test
    void onConnect_ignoresBlankUserId() {
        SocketIOClient client = mock(SocketIOClient.class);
        HandshakeData handshake = mock(HandshakeData.class);
        when(client.getHandshakeData()).thenReturn(handshake);
        when(handshake.getSingleUrlParam("userId")).thenReturn("  ");

        connectListener.onConnect(client);

        // Nothing was registered, so a later send finds no session.
        handler.sendToUser(7L, "ping", Map.of());
        verify(server, never()).getClient(any());
    }

    // ---- onSendMessage -----------------------------------------------------

    @Test
    void onSendMessage_persistsAndSendsAckWhenRequested() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setChatId(1L);
        request.setMemberId(2L);
        request.setMessage("hi");
        ChatMessageDto saved = messageFrom(2L);
        when(chatMessageService.sendMessage(request)).thenReturn(saved);

        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ack = mock(AckRequest.class);
        when(ack.isAckRequested()).thenReturn(true);

        this.<SendMessageRequest>listenerFor(SocketEvent.SEND_MESSAGE).onData(client, request, ack);

        verify(chatMessageService).sendMessage(request);
        verify(ack).sendAckData(saved);
    }

    @Test
    void onSendMessage_onFailureNotifiesClientAndRethrows() {
        SendMessageRequest request = new SendMessageRequest();
        request.setChatId(1L);
        when(chatMessageService.sendMessage(request)).thenThrow(new RuntimeException("boom"));

        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ack = mock(AckRequest.class);

        assertThatThrownBy(() ->
                this.<SendMessageRequest>listenerFor(SocketEvent.SEND_MESSAGE).onData(client, request, ack))
                .isInstanceOf(SocketConnectionException.class);

        verify(client).sendEvent("error", Map.of("message", "Failed to send message"));
    }

    // ---- onJoinChat --------------------------------------------------------

    @Test
    void onJoinChat_joinsRoomPublishesPresenceAndAcks() throws Exception {
        SocketIOClient client = clientWithUser(5L, UUID.randomUUID());
        AckRequest ack = mock(AckRequest.class);
        when(ack.isAckRequested()).thenReturn(true);

        this.<Long>listenerFor(SocketEvent.JOIN_CHAT).onData(client, 1L, ack);

        verify(client).joinRoom("chat_1");
        verify(eventPublisher).publishUserJoined(1L, 5L);
        verify(ack).sendAckData(Map.of("status", "joined", "chatId", 1L));
    }

    // ---- onDeleteMessage ---------------------------------------------------

    @Test
    void onDeleteMessage_deletesAndAcks() throws Exception {
        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ack = mock(AckRequest.class);
        when(ack.isAckRequested()).thenReturn(true);

        this.<Long>listenerFor(SocketEvent.DELETE_MESSAGE).onData(client, 5L, ack);

        verify(chatMessageService).deleteChatMessage(5L);
        verify(ack).sendAckData(Map.of("status", "deleted", "messageId", 5L));
    }

    // ---- onChatOpenedAck ---------------------------------------------------

    @Test
    void onChatOpenedAck_joinsRoom() throws Exception {
        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ack = mock(AckRequest.class);

        this.<ChatOpenedAckRequest>listenerFor(SocketEvent.CHAT_OPENED_ACK)
                .onData(client, new ChatOpenedAckRequest(3L), ack);

        verify(client).joinRoom("chat_3");
    }

    @Test
    void onChatOpenedAck_nullChatIdDoesNothing() throws Exception {
        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ack = mock(AckRequest.class);

        this.<ChatOpenedAckRequest>listenerFor(SocketEvent.CHAT_OPENED_ACK)
                .onData(client, new ChatOpenedAckRequest(null), ack);

        verify(client, never()).joinRoom(any());
    }

    // ---- onMessagesRead ----------------------------------------------------

    @Test
    void onMessagesRead_marksReadAndBroadcastsToRoom() throws Exception {
        SocketIOClient client = clientWithUser(5L, UUID.randomUUID());
        AckRequest ack = mock(AckRequest.class);
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);
        when(chatMessageService.markAsRead(1L, 5L, "READ")).thenReturn(List.of(10L, 11L));

        this.<MessagesReadRequest>listenerFor(SocketEvent.MESSAGES_READ)
                .onData(client, new MessagesReadRequest(1L), ack);

        verify(chatMessageService).markAsRead(1L, 5L, "READ");
        verify(room).sendEvent("messages_read",
                Map.of("userId", 5L, "messageIds", List.of(10L, 11L)));
    }

    // ---- handleChatEvent: NEW_MESSAGE -------------------------------------

    @Test
    void handleChatEvent_newMessage_broadcastsToRoomExceptSenderAndNotifiesAbsentMembers() {
        long chatId = 1L;
        ChatMessageDto message = messageFrom(10L); // sender = 10

        // Member 30 is connected but not in the room -> should get a notification.
        UUID session30 = UUID.randomUUID();
        SocketIOClient client30 = clientWithUser(30L, session30);
        connectListener.onConnect(client30);
        when(server.getClient(session30)).thenReturn(client30);

        // Room contains the sender (10) and member 20.
        SocketIOClient sender = clientWithUser(10L, UUID.randomUUID());
        SocketIOClient client20 = clientWithUser(20L, UUID.randomUUID());
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);
        when(room.getClients()).thenReturn(List.of(sender, client20));

        when(chatMemberService.getMembersByChatId(chatId)).thenReturn(List.of(10L, 20L, 30L));

        handler.handleChatEvent(new ChatEvent(this, EventType.NEW_MESSAGE, chatId,
                Map.of("message", message)));

        // In-room, not the sender -> direct broadcast.
        verify(client20).sendEvent("new_message", message);
        // Sender never receives their own message back.
        verify(sender, never()).sendEvent("new_message", message);
        // Connected member absent from the room -> notification.
        verify(client30).sendEvent(SocketEvent.MESSAGE_NOTIFICATION.value(), message);
    }

    @Test
    void handleChatEvent_newMessage_noMembersDoesNothing() {
        ChatMessageDto message = messageFrom(10L);
        when(chatMemberService.getMembersByChatId(1L)).thenReturn(List.of());

        handler.handleChatEvent(new ChatEvent(this, EventType.NEW_MESSAGE, 1L,
                Map.of("message", message)));

        verify(server, never()).getRoomOperations(any(String.class));
    }

    // ---- handleChatEvent: room broadcasts ---------------------------------

    @Test
    void handleChatEvent_messageUpdated_broadcastsToRoom() {
        ChatMessageDto message = messageFrom(10L);
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);

        handler.handleChatEvent(new ChatEvent(this, EventType.MESSAGE_UPDATED, 1L,
                Map.of("message", message)));

        verify(room).sendEvent("message_updated", message);
    }

    @Test
    void handleChatEvent_messageDeleted_broadcastsMessageIdToRoom() {
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);

        handler.handleChatEvent(new ChatEvent(this, EventType.MESSAGE_DELETED, 1L,
                Map.of("messageId", 42L)));

        verify(room).sendEvent("message_deleted", Map.of("messageId", 42L));
    }

    @Test
    void handleChatEvent_chatArchived_broadcastsToRoom() {
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);

        handler.handleChatEvent(new ChatEvent(this, EventType.CHAT_ARCHIVED, 1L,
                Map.of("chatId", 1L)));

        verify(room).sendEvent("chat_archived", Map.of("chatId", 1L));
    }

    @Test
    void handleChatEvent_userJoined_broadcastsPresenceToRoom() {
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);

        handler.handleChatEvent(new ChatEvent(this, EventType.USER_JOINED, 1L,
                Map.of("userId", 9L)));

        verify(room).sendEvent("user_joined", Map.of("userId", 9L));
    }

    // ---- handleChatEvent: CHAT_OPENED -------------------------------------

    @Test
    void handleChatEvent_chatOpened_sendsChatDataToEachUser() {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        SocketIOClient c1 = clientWithUser(1L, s1);
        SocketIOClient c2 = clientWithUser(2L, s2);
        connectListener.onConnect(c1);
        connectListener.onConnect(c2);
        when(server.getClient(s1)).thenReturn(c1);
        when(server.getClient(s2)).thenReturn(c2);

        Map<String, Object> chatData = Map.of("chatId", 1L, "name", "Chat 1");

        handler.handleChatEvent(new ChatEvent(this, EventType.CHAT_OPENED, 1L,
                Map.of("userIds", List.of(1L, 2L), "data", chatData)));

        verify(c1).sendEvent("chat_opened", chatData);
        verify(c2).sendEvent("chat_opened", chatData);
    }

    @Test
    void handleChatEvent_swallowsExceptionsFromBroadcasting() {
        ChatMessageDto message = messageFrom(10L);
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(server.getRoomOperations("chat_1")).thenReturn(room);
        doThrow(new RuntimeException("socket down")).when(room).sendEvent(any(), any());

        assertThatCode(() -> handler.handleChatEvent(
                new ChatEvent(this, EventType.MESSAGE_UPDATED, 1L, Map.of("message", message))))
                .doesNotThrowAnyException();
    }

    // ---- sendToUser --------------------------------------------------------

    @Test
    void sendToUser_nullUserIdIsIgnored() {
        handler.sendToUser(null, "evt", Map.of());
        verify(server, never()).getClient(any());
    }

    @Test
    void sendToUser_unknownUserIsIgnored() {
        handler.sendToUser(999L, "evt", Map.of());
        verify(server, never()).getClient(any());
    }
}
