package am.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateMessageRequest(

        @Positive(message = "Message ID must be positive")
        long messageId,

        @NotBlank(message = "Message cannot be blank")
        @NotNull(message = "Message cannot be null")
        String message

) {
}
