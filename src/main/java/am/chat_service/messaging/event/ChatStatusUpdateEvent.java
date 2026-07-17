package am.chat_service.messaging.event;

public record ChatStatusUpdateEvent(
        long chatId,
        String status
) {
}
