package am.chat_service.dto.request;

import am.chat_service.model.enums.ChatType;

import java.util.List;

public record CreateChatRequest(
        List<Long> userIds,
        ChatType chatType
) {
}
