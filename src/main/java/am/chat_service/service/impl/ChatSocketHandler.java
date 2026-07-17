package am.chat_service.service.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatOpenedAckRequest;
import am.chat_service.dto.request.MessageDeliveredRequest;
import am.chat_service.dto.request.MessagesReadRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.event.ChatEvent;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.SocketConnectionException;
import am.chat_service.model.enums.MessageStatus;
import am.chat_service.model.enums.SocketEvent;
import am.chat_service.service.ChatMemberService;
import am.chat_service.service.ChatMessageService;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSocketHandler {

    private final SocketIOServer server;
    private final ChatMessageService chatMessageService;
    private final ChatEventPublisher eventPublisher;
    private final ChatMemberService chatMemberService;

    private final Map<Long, UUID> userSocketMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerEventListeners() {
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(this::onDisconnect);

        server.addEventListener(SocketEvent.SEND_MESSAGE.value(), SendMessageRequest.class, this::onSendMessage);
        server.addEventListener(SocketEvent.UPDATE_MESSAGE.value(), UpdateMessageRequest.class, this::onUpdateMessage);
        server.addEventListener(SocketEvent.DELETE_MESSAGE.value(), Long.class, this::onDeleteMessage);
        server.addEventListener(SocketEvent.JOIN_CHAT.value(), Long.class, this::onJoinChat);
        server.addEventListener(SocketEvent.LEAVE_CHAT.value(), Long.class, this::onLeaveChat);

        server.addEventListener(SocketEvent.CHAT_OPENED_ACK.value(), ChatOpenedAckRequest.class, this::onChatOpenedAck);
        server.addEventListener(SocketEvent.MESSAGES_READ.value(), MessagesReadRequest.class, this::onMessagesRead);
        server.addEventListener(SocketEvent.MESSAGE_DELIVERED.value(), MessageDeliveredRequest.class, this::onMessageDelivered);
    }


    private void onConnect(SocketIOClient client) {
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");

        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("Client connected without userId: %s".formatted(client.getSessionId()));
            return;
        }
        try {
            Long userId = Long.parseLong(userIdStr.trim());
            userSocketMap.put(userId, client.getSessionId());
            log.info("✅ User %d connected. SessionId: %s".formatted(userId, client.getSessionId()));
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format: %s".formatted(userIdStr));
        } catch (Exception e) {
            log.error("Error in onConnect for userId: %s".formatted(userIdStr), e);
        }
    }

    private void onDisconnect(SocketIOClient client) {
        userSocketMap.entrySet().removeIf(entry ->
                entry.getValue().equals(client.getSessionId())
        );
        log.info("Client disconnected: {}", client.getSessionId());
    }


    private void onJoinChat(SocketIOClient client, Long chatId, AckRequest ackSender) {
        try {
            client.joinRoom(buildRoom(chatId));
            Long userId = getUserIdFromClient(client);
            eventPublisher.publishUserJoined(chatId, userId);
            log.info("Client {} joined room: chat_{}", client.getSessionId(), chatId);

            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(Map.of("status", "joined", "chatId", chatId));
            }
        } catch (Exception e) {
            log.error("Error joining chat {}", chatId, e);
            client.sendEvent("error", Map.of("message", "Failed to join chat"));
        }
    }

    private void onLeaveChat(SocketIOClient client, Long chatId, AckRequest ackSender) {
        try {
            client.leaveRoom(buildRoom(chatId));
            Long userId = getUserIdFromClient(client);
            eventPublisher.publishUserLeft(chatId, userId);
            log.info("Client {} left room: chat_{}", client.getSessionId(), chatId);
        } catch (Exception e) {
            log.error("Error leaving chat {}", chatId, e);
        }
    }

    private void onSendMessage(SocketIOClient client, SendMessageRequest request, AckRequest ackSender) {
        try {
            ChatMessageDto savedMessage = chatMessageService.sendMessage(request);

            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(savedMessage);
            }

            log.info("Message sent in chat {} by member {}", request.getChatId(), request.getMemberId());
        } catch (Exception e) {
            log.error("Failed to send message in chat {}", request.getChatId(), e);
            client.sendEvent("error", Map.of("message", "Failed to send message"));
            throw new SocketConnectionException("Failed to process send_message event", e);
        }
    }

    private void onUpdateMessage(SocketIOClient client, UpdateMessageRequest request, AckRequest ackSender) {
        try {
            ChatMessageDto updated = chatMessageService.updateChatMessage(request);

            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(updated);
            }

            log.info("Message {} updated", request.messageId());
        } catch (Exception e) {
            log.error("Failed to update message {}", request.messageId(), e);
            client.sendEvent("error", Map.of("message", "Failed to update message"));
        }
    }

    private void onDeleteMessage(SocketIOClient client, Long messageId, AckRequest ackSender) {
        try {
            chatMessageService.deleteChatMessage(messageId);

            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(Map.of("status", "deleted", "messageId", messageId));
            }

            log.info("Message {} deleted", messageId);
        } catch (Exception e) {
            log.error("Failed to delete message {}", messageId, e);
            client.sendEvent("error", Map.of("message", "Failed to delete message"));
        }
    }

    private void onChatOpenedAck(SocketIOClient client, ChatOpenedAckRequest request, AckRequest ackSender) {
        try {
            if (request.chatId() == null) return;

            client.joinRoom(buildRoom(request.chatId()));
            log.info("Client acknowledged chat opened and joined room: {}", request.chatId());
        } catch (Exception e) {
            log.warn("Failed to process chat_opened_ack", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleChatEvent(ChatEvent event) {
        try {
            switch (event.getEventType()) {
                case CHAT_OPENED -> handleChatOpenedEvent(event);
                case NEW_MESSAGE -> handleNewMessageEvent(event);
                case MESSAGE_UPDATED -> handleMessageUpdatedEvent(event);
                case MESSAGE_DELETED -> handleMessageDeletedEvent(event);
                case CHAT_ARCHIVED -> handleChatArchivedEvent(event);
                case USER_JOINED, USER_LEFT -> handleUserPresenceEvent(event);
            }
        } catch (Exception e) {
            log.error("Error handling ChatEvent: {}", event.getEventType(), e);
        }
    }

    private void handleChatOpenedEvent(ChatEvent event) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = (List<Long>) event.getPayload().get("userIds");
        @SuppressWarnings("unchecked")
        Map<String, Object> chatData = (Map<String, Object>) event.getPayload().get("data");

        if (userIds == null) return;

        for (Long userId : userIds) {
            sendToUser(userId, event.getEventType().value(), chatData);
        }
    }

    private void handleNewMessageEvent(ChatEvent event) {
        Long chatId = event.getChatId();
        ChatMessageDto message = extractMessage(event);

        if (message == null) return;

        List<Long> memberIds = chatMemberService.getMembersByChatId(chatId);
        if (memberIds == null || memberIds.isEmpty()) return;

        Long senderId = message.userId();
        Set<Long> usersInRoom = roomUserIds(chatId);

        broadcastToRoomExceptSender(chatId, event.getEventType().value(), message, senderId);

        for (Long memberId : memberIds) {
            if (senderId != null && senderId.equals(memberId)) continue;
            if (usersInRoom.contains(memberId)) continue;
            if (userSocketMap.containsKey(memberId)) {
                sendToUser(memberId, SocketEvent.MESSAGE_NOTIFICATION.value(), message);
            }
        }
    }

    private void handleMessageUpdatedEvent(ChatEvent event) {
        ChatMessageDto message = extractMessage(event);
        if (message == null) return;

        sendToRoom(event.getChatId(), event.getEventType().value(), message);
    }

    private void handleMessageDeletedEvent(ChatEvent event) {
        Object messageId = event.getPayload().get("messageId");
        if (messageId == null) return;

        sendToRoom(event.getChatId(), event.getEventType().value(), Map.of("messageId", messageId));
    }

    private void handleChatArchivedEvent(ChatEvent event) {
        sendToRoom(event.getChatId(), event.getEventType().value(), Map.of("chatId", event.getChatId()));
    }

    private void handleUserPresenceEvent(ChatEvent event) {
        Long chatId = event.getChatId();
        Long userId = (Long) event.getPayload().get("userId");

        if (userId == null) return;

        String eventName = event.getEventType().value();

        if (eventName != null) {
            sendToRoom(chatId, eventName, Map.of("userId", userId));
        }
    }

    public void sendToUser(Long userId, String event, Object data) {
        if (userId == null) {
            log.warn("sendToUser skipped: userId is null for event {}", event);
            return;
        }
        UUID sessionId = userSocketMap.get(userId);

        if (sessionId == null) {
            log.warn("User {} is not connected", userId);
            return;
        }

        SocketIOClient client = server.getClient(sessionId);

        if (client == null) {
            log.warn("Client not found for user {}", userId);
            return;
        }

        client.sendEvent(event, data);
    }

    private String buildRoom(Long chatId) {
        return "chat_" + chatId;
    }

    private void sendToRoom(Long chatId, String event, Object data) {
        server.getRoomOperations(buildRoom(chatId)).sendEvent(event, data);
    }

    private void broadcastToRoomExceptSender(Long chatId, String event, Object data, Long senderId) {
        for (SocketIOClient roomClient : server.getRoomOperations(buildRoom(chatId)).getClients()) {
            Long roomUserId = roomClientUserId(roomClient);

            if (roomUserId == null) continue;
            if (senderId != null && roomUserId.equals(senderId)) continue;

            roomClient.sendEvent(event, data);
        }
    }

    private Set<Long> roomUserIds(Long chatId) {
        Set<Long> ids = new HashSet<>();
        for (SocketIOClient roomClient : server.getRoomOperations(buildRoom(chatId)).getClients()) {
            Long roomUserId = roomClientUserId(roomClient);
            if (roomUserId != null) {
                ids.add(roomUserId);
            }
        }
        return ids;
    }

    private Long roomClientUserId(SocketIOClient client) {
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");

        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void onMessageDelivered(SocketIOClient client, MessageDeliveredRequest request, AckRequest ackSender) {
        try {
            Long messageId = request.messageId();

            chatMessageService.markAsDelivered(messageId);

            ChatMessageDto message = chatMessageService.getMessageById(messageId);

            if (message.userId() != null) {
                sendToUser(message.userId(), SocketEvent.MESSAGE_DELIVERED.value(),
                        Map.of("messageId", messageId));
            }

        } catch (Exception e) {
            log.error("Failed to mark message as delivered", e);
        }
    }

    private void onMessagesRead(SocketIOClient client, MessagesReadRequest request, AckRequest ackSender) {
        try {
            Long chatId = request.chatId();
            Long userId = getUserIdFromClient(client);

            List<Long> messageIds = chatMessageService.markAsRead(chatId, userId, MessageStatus.READ.name());

            sendToRoom(chatId, "messages_read", Map.of(
                    "userId", userId,
                    "messageIds", messageIds
            ));
            log.info("Message status changed");
        } catch (Exception e) {
            log.error("Failed to mark messages as read", e);
        }
    }

    private Long getUserIdFromClient(SocketIOClient client) {
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");
        return Long.parseLong(userIdStr);
    }

    private ChatMessageDto extractMessage(ChatEvent event) {
        return (ChatMessageDto) event.getPayload().get("message");
    }

}