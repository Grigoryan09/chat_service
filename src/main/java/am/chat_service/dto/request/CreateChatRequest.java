package am.chat_service.dto.request;

import am.chat_service.model.enums.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateChatRequest(

        @NotEmpty(message = "User list cannot be empty")
        @Size(min = 2, message = "A chat must have at least 2 participants")
        List<Long> userIds,

        @NotNull(message = "Chat type is required")
        ChatType chatType
) {
}
