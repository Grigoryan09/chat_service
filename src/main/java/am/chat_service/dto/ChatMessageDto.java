package am.chat_service.dto;

import am.chat_service.model.enums.MessageStatus;

import java.time.LocalDateTime;

public record ChatMessageDto(

        Long id,
        Long userId,
        String message,
        MessageStatus status,
        LocalDateTime createdDate,
        LocalDateTime updatedDate) {
}


