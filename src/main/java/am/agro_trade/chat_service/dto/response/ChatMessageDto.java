package am.agro_trade.chat_service.dto.response;

import java.time.LocalDateTime;

public record ChatMessageDto(
       long id,
       long userId,
       String message,
       boolean isRead,
       LocalDateTime createdDate,
       LocalDateTime updatedDate){
}


