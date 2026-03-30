package am.chat_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatDto(

        long id,
        LocalDateTime createdDateTime,
        String chatType,
        String chatStatus,
        List<ChatMessageDto> messages,
        List<ChatMemberDto> members
) {
}