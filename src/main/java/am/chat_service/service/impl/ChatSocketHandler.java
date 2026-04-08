package am.chat_service.service.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatOpenedAckRequest;
import am.chat_service.dto.request.MessageDeliveredRequest;
import am.chat_service.dto.request.MessagesReadRequest;
import am.chat_service.dto.request.SendMessageRequest;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

    private void onChatOpenedAck(SocketIOClient client, ChatOpenedAckRequest request, AckRequest ackSender) {
        try {
            if (request.chatId() == null) return;

            client.joinRoom(buildRoom(request.chatId()));
            log.info("Client acknowledged chat opened and joined room: {}", request.chatId());
        } catch (Exception e) {
            log.warn("Failed to process chat_opened_ack", e);
        }
    }

    @EventListener
    public void handleChatEvent(ChatEvent event) {
        try {
            switch (event.getEventType()) {
                case CHAT_OPENED -> handleChatOpenedEvent(event);
                case NEW_MESSAGE -> handleNewMessageEvent(event);
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
        ChatMessageDto message = (ChatMessageDto) event.getPayload().get("message");
        List<Long> userIds = chatMemberService.getMembersByChatId(chatId);

        if (message != null && userIds != null) {
            List<Long> connectedUsers = userIds.stream()
                    .filter(userSocketMap::containsKey)
                    .toList();

            List<Long> offlineUsers = userIds.stream()
                    .filter(userId -> !userSocketMap.containsKey(userId))
                    .toList();
            if (!connectedUsers.isEmpty()) {
                sendToRoomExceptUser(chatId,
                        message.userId(),
                        event.getEventType().value(),
                        message,
                        connectedUsers);
            }
            if (!offlineUsers.isEmpty()) {
                notifyOfflineUsers(chatId, offlineUsers, message);
            }
        }
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

    private void sendToRoomExceptUser(Long chatId,
                                      Long excludedUserId,
                                      String event,
                                      Object data,
                                      List<Long> connectedUsers) {
        for (SocketIOClient roomClient : server.getRoomOperations(buildRoom(chatId)).getClients()) {
            String userIdStr = roomClient.getHandshakeData().getSingleUrlParam("userId");

            if (userIdStr == null || userIdStr.isBlank()) {
                continue;
            }

            Long roomUserId;
            try {
                roomUserId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (!roomUserId.equals(excludedUserId) && connectedUsers.contains(roomUserId)) {
                roomClient.sendEvent(event, data);
            }
        }
    }

    private void notifyOfflineUsers(Long chatId, List<Long> userIds, ChatMessageDto message) {
        for (Long userId : userIds) {
            UUID sessionId = userSocketMap.get(userId);

            if (sessionId == null) continue;

            SocketIOClient client = server.getClient(sessionId);

            if (client == null) continue;

            boolean inRoom = client.getAllRooms().contains(buildRoom(chatId));

            if (!inRoom) {
                sendToUser(userId, SocketEvent.MESSAGE_NOTIFICATION.value(), message);
            }
        }
    }

    private void onMessageDelivered(SocketIOClient client, MessageDeliveredRequest request, AckRequest ackSender) {
        try {
            Long messageId = request.messageId();

            chatMessageService.markAsDelivered(messageId);

            ChatMessageDto message = chatMessageService.getMessageById(messageId);

            sendToUser(message.userId(), SocketEvent.MESSAGE_DELIVERED.value(),
                    Map.of("messageId", messageId));

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

}
