package am.chat_service.dto.request;

import am.chat_service.model.enums.ChatType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateChatRequest(

        @ArraySchema(
                schema = @Schema(implementation = Long.class),
                arraySchema = @Schema(
                        description = "List of participant user IDs for the new chat",
                        example = "[101, 202]"
                )
        )
        @NotEmpty(message = "User list cannot be empty")
        @Size(min = 2, message = "A chat must have at least 2 participants")
        List<Long> userIds,

        @Schema(
                description = "Type of chat to create",
                example = "PRIVATE"
        )
        @NotNull(message = "Chat type is required")
        ChatType chatType
) {
}
