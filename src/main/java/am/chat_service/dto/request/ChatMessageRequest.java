package am.chat_service.dto.request;

import jakarta.validation.constraints.Positive;

public record ChatMessageRequest(

        @Positive(message = "Chat ID must be positive")
        long chatId,

        @Positive(message = "Member ID must be positive")
        long memberId

) {
}