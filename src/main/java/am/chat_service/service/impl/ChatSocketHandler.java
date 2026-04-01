package am.chat_service.service.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.event.ChatEvent;
import am.chat_service.exception.SocketConnectionException;
import am.chat_service.model.enums.MessageStatus;
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

    private final Map<Long, UUID> userSocketMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerEventListeners() {
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(this::onDisconnect);

        server.addEventListener("send_message", SendMessageRequest.class, this::onSendMessage);
        server.addEventListener("join_chat", Long.class, this::onJoinChat);
        server.addEventListener("leave_chat", Long.class, this::onLeaveChat);
        server.addEventListener("chat_opened_ack", Map.class, this::onChatOpenedAck);
        server.addEventListener("messages_read", Map.class, this::onMessagesRead);
        server.addEventListener("message_delivered", Map.class, this::onMessageDelivered);
    }


    private void onConnect(SocketIOClient client) {
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");

        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("Client connected without userId: {}", client.getSessionId());
            return;
        }

        try {
            Long userId = Long.parseLong(userIdStr.trim());
            userSocketMap.put(userId, client.getSessionId());
            log.info("✅ User {} connected. SessionId: {}", userId, client.getSessionId());
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format: {}", userIdStr);
        } catch (Exception e) {
            log.error("Error in onConnect", e);
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
            log.info("Client {} left room: chat_{}", client.getSessionId(), chatId);
        } catch (Exception e) {
            log.error("Error leaving chat {}", chatId, e);
        }
    }

    private void onSendMessage(SocketIOClient client, SendMessageRequest request, AckRequest ackSender) {
        try {
            ChatMessageDto savedMessage = chatMessageService.sendMessage(request);

            sendToRoom(request.getChatId(), "new_message", savedMessage);

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

    private void onChatOpenedAck(SocketIOClient client, Map<String, Object> data, AckRequest ackSender) {
        try {
            if (data == null || !data.containsKey("chatId")) {
                return;
            }

            Long chatId = ((Number) data.get("chatId")).longValue();
            client.joinRoom(buildRoom(chatId));
            log.info("Client acknowledged chat opened and joined room: {}", chatId);
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
            sendToUser(userId, "chat_opened", chatData);
        }
    }

    private void handleNewMessageEvent(ChatEvent event) {
        Long chatId = event.getChatId();
        ChatMessageDto message = (ChatMessageDto) event.getPayload().get("message");
        @SuppressWarnings("unchecked")
        List<Long> userIds = (List<Long>) event.getPayload().get("userIds");

        if (message != null) {
            sendToRoom(chatId, "new_message", message);
            if (userIds != null) {
                notifyOfflineUsers(chatId, userIds, message);
            }
        }
    }

    private void handleUserPresenceEvent(ChatEvent event) {
        log.info("User presence event received: {} in chat {}",
                event.getEventType(), event.getChatId());
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

    private void notifyOfflineUsers(Long chatId, List<Long> userIds, ChatMessageDto message) {
        for (Long userId : userIds) {
            UUID sessionId = userSocketMap.get(userId);

            if (sessionId == null) continue;

            SocketIOClient client = server.getClient(sessionId);

            if (client == null) continue;

            boolean inRoom = client.getAllRooms().contains(buildRoom(chatId));

            if (!inRoom) {
                sendToUser(userId, "new_message_notification", message);
            }
        }
    }

    private void onMessageDelivered(SocketIOClient client, Map<String, Object> data, AckRequest ackSender) {
        try {
            Long messageId = ((Number) data.get("messageId")).longValue();

            chatMessageService.markAsDelivered(messageId);

            ChatMessageDto message = chatMessageService.getMessageById(messageId);

            sendToUser(
                    message.userId(),
                    "message_delivered",
                    Map.of("messageId", messageId)
            );

        } catch (Exception e) {
            log.error("Failed to mark message as delivered", e);
        }
    }

    private void onMessagesRead(SocketIOClient client, Map<String, Object> data, AckRequest ackSender) {
        try {
            Long chatId = ((Number) data.get("chatId")).longValue();
            Long userId = getUserIdFromClient(client);

            List<Long> messageIds = chatMessageService.markAsRead(chatId, userId, MessageStatus.READ);

            sendToRoom(chatId, "messages_read", Map.of(
                    "userId", userId,
                    "messageIds", messageIds
            ));

        } catch (Exception e) {
            log.error("Failed to mark messages as read", e);
        }
    }

    private Long getUserIdFromClient(SocketIOClient client) {
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");
        return Long.parseLong(userIdStr);
    }

}