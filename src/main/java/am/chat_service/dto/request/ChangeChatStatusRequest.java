package am.chat_service.dto.request;

import am.chat_service.model.enums.ChatStatus;

public record ChangeChatStatusRequest(
        long chatId,
        ChatStatus chatStatus
) {
}
