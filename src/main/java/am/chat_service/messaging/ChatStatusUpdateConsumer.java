package am.chat_service.messaging;

import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.messaging.event.ChatStatusUpdateEvent;
import am.chat_service.messaging.event.OrderDocumentRequestEvent;
import am.chat_service.model.enums.ChatStatus;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatStatusUpdateConsumer {

    private final ChatService chatService;
    private final ChatKafkaProducer chatKafkaProducer;

    @KafkaListener(
            topics = "${kafka.topics.chat-status-update}",
            containerFactory = "chatStatusKafkaListenerContainerFactory")
    public void onChatStatusUpdate(ChatStatusUpdateEvent event) {
        log.info("Received chat status update for chat {}: {}", event.chatId(), event.status());

        if (event.chatId() <= 0) {
            log.error("Ignoring chat status update with invalid chatId={}. The producing service "
                            + "failed to attach a chat to its order; the order document chain "
                            + "cannot start for this event.",
                    event.chatId());
            return;
        }

        ChatStatus status = resolveStatus(event.status());
        if (status == null) {
            return;
        }

        try {
            chatService.changeChatStatus(new ChangeChatStatusRequest(event.chatId(), status));
        } catch (Exception e) {
            log.error("Failed to change status of chat {} to {}", event.chatId(), status, e);
        }

        if (status == ChatStatus.COMPLETED) {
            chatKafkaProducer.sendOrderDocumentRequest(new OrderDocumentRequestEvent(event.chatId()));
            log.info("Requested order document generation for chat {}", event.chatId());
        }
    }

    private ChatStatus resolveStatus(String status) {
        try {
            return ChatStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Unknown chat status '{}' received, ignoring event", status);
            return null;
        }
    }
}
