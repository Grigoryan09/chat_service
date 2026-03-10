package am.agro_trade.chat_service.dto.request;

import am.agro_trade.chat_service.model.enums.ChatType;

public record CreateChatRequest(
        long userId,
        ChatType chatType
) {
}
