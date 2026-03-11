package am.agro_trade.chat_service.dto.request;

import am.agro_trade.chat_service.model.enums.ChatStatus;

public record ChangeChatStatusRequest(
        long chatId,
        ChatStatus chatStatus
) {
}
