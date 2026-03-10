package am.agro_trade.chat_service.dto.request;

import java.time.LocalDateTime;


public record SendMessageRequest(
        long chatId,
        long userId,
        String message,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        boolean isRead
) {
}
