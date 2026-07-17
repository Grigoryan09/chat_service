package am.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequest(

        @NotBlank(message = "Message cannot be blank")
        String message
) {
}