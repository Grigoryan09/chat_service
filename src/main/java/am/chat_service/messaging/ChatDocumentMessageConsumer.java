package am.chat_service.messaging;

import am.chat_service.messaging.event.ChatDocumentMessageEvent;
import am.chat_service.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatDocumentMessageConsumer {

    private final ChatMessageService chatMessageService;

    @KafkaListener(
            topics = "${kafka.topics.chat-document-message}",
            containerFactory = "chatDocumentKafkaListenerContainerFactory")
    public void onChatDocumentMessage(ChatDocumentMessageEvent event) {
        log.info("Received document message for chat {}: {}", event.chatId(), event.fileName());

        String message = "%s\n%s".formatted(event.fileName(), event.fileUrl());
        chatMessageService.sendDocumentMessage(event.chatId(), event.senderUserId(), message);
    }
}
