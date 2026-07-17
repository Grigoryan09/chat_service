package am.chat_service.messaging.event;

public record ChatDocumentMessageEvent(
        long chatId,
        long senderUserId,
        String fileName,
        String fileUrl
) {
}
