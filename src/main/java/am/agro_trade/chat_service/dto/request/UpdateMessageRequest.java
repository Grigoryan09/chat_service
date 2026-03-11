package am.agro_trade.chat_service.dto.request;

import java.time.LocalDateTime;

public record UpdateMessageRequest(
        long userId,
        long messageId,
        String message,
        LocalDateTime updatedDate,
        long chatId,
        boolean isRead
) {
}
