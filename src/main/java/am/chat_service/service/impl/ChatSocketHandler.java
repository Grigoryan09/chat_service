package am.chat_service.service.impl;

import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.response.ChatMessageDto;
import am.chat_service.event.ChatEvent;
import am.chat_service.exception.SocketConnectionException;
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
            String room = "chat_" + chatId;
            client.joinRoom(room);
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
            String room = "chat_" + chatId;
            client.leaveRoom(room);
            log.info("Client {} left room: chat_{}", client.getSessionId(), chatId);
        } catch (Exception e) {
            log.error("Error leaving chat {}", chatId, e);
        }
    }

    private void onSendMessage(SocketIOClient client, SendMessageRequest request, AckRequest ackSender) {
        try {
            ChatMessageDto savedMessage = chatMessageService.sendMessage(request);

            String room = "chat_" + request.getChatId();
            server.getRoomOperations(room).sendEvent("new_message", savedMessage);

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
            String room = "chat_" + chatId;
            client.joinRoom(room);
            log.info("Client acknowledged chat opened and joined room: {}", chatId);
        } catch (Exception e) {
            log.warn("Failed to process chat_opened_ack", e);
        }
    }


    @EventListener
    public void handleChatEvent(ChatEvent event) {
        try {
            switch (event.getEventType()) {
                case "chat_opened" -> handleChatOpenedEvent(event);
                case "new_message" -> handleNewMessageEvent(event);
                case "user_joined", "user_left" -> handleUserPresenceEvent(event);
            }
        } catch (Exception e) {
            log.error("Error handling ChatEvent: {}", event.getEventType(), e);
        }
    }

    private void handleChatOpenedEvent(ChatEvent event) {
        Long chatId = event.getChatId();
        @SuppressWarnings("unchecked")
        List<Long> userIds = (List<Long>) event.getPayload().get("userIds");
        @SuppressWarnings("unchecked")
        Map<String, Object> chatData = (Map<String, Object>) event.getPayload().get("data");

        if (userIds == null) return;

        for (Long userId : userIds) {
            UUID sessionId = userSocketMap.get(userId);
            if (sessionId != null) {
                try {
                    server.getClient(sessionId).sendEvent("chat_opened", chatData);
                    log.info("Sent 'chat_opened' to user {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to send chat_opened to user {}", userId, e);
                }
            }
        }
    }

    private void handleNewMessageEvent(ChatEvent event) {
        Long chatId = event.getChatId();
        @SuppressWarnings("unchecked")
        ChatMessageDto message = (ChatMessageDto) event.getPayload().get("message");

        if (message != null) {
            String room = "chat_" + chatId;
            server.getRoomOperations(room).sendEvent("new_message", message);
        }
    }

    private void handleUserPresenceEvent(ChatEvent event) {
        log.info("User presence event received: {} in chat {}",
                event.getEventType(), event.getChatId());
    }


    public void sendToUser(Long userId, String eventName, Object data) {
        UUID sessionId = userSocketMap.get(userId);
        if (sessionId != null) {
            try {
                server.getClient(sessionId).sendEvent(eventName, data);
            } catch (Exception e) {
                log.warn("Failed to send {} to user {}", eventName, userId, e);
            }
        }
    }

}