package am.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Chat ID cannot be null")
    long chatId;

    @NotNull(message = "Member ID cannot be null")
    long memberId;

    @NotBlank(message = "Message cannot be blank")
    String message;

}
