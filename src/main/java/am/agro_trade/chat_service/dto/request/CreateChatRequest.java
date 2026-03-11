package am.agro_trade.chat_service.dto.request;

import am.agro_trade.chat_service.model.enums.ChatType;

import java.util.List;

public record CreateChatRequest(
        List<Long> userIds,
        ChatType chatType
) {
}
