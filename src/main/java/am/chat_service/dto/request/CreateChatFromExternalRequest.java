package am.chat_service.dto.request;

import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateChatFromExternalRequest {

    @NotEmpty(message = "Нужен хотя бы 2 участник")
    @Size(min = 2, message = "Минимум 2 участника для чата")
    private List<Long> userIds;
    private String chatName;
    private ChatType chatType;
    private ChatStatus chatStatus = ChatStatus.ACTIVE;
}