package am.chat_service.dto;

import am.chat_service.model.enums.MessageStatus;

import java.time.LocalDateTime;

public record ChatMessageDto(

        long id,
        long userId,
        String message,
        MessageStatus status,
        LocalDateTime createdDate,
        LocalDateTime updatedDate) {
}


