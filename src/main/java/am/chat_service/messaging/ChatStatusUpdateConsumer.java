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

        ChatStatus status = ChatStatus.valueOf(event.status());
        chatService.changeChatStatus(new ChangeChatStatusRequest(event.chatId(), status));

        if (status == ChatStatus.COMPLETED) {
            chatKafkaProducer.sendOrderDocumentRequest(new OrderDocumentRequestEvent(event.chatId()));
        }
    }
}
