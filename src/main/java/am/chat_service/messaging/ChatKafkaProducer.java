package am.chat_service.messaging;

import am.chat_service.messaging.event.OrderDocumentRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-document-request}")
    private String orderDocumentRequestTopic;

    public void sendOrderDocumentRequest(OrderDocumentRequestEvent event) {
        kafkaTemplate.send(orderDocumentRequestTopic, String.valueOf(event.chatId()), event);
    }
}
