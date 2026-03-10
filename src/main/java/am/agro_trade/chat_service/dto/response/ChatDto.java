package am.agro_trade.chat_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatDto(
        long id,
        LocalDateTime createdDateTime,
        LocalDateTime lastActivity,
        String chatType,
        String chatStatus,
        List<ChatMemberDto> members
) {
}