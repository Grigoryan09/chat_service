package am.chat_service.dto;

import org.springframework.data.domain.Page;

public record UserChatsDto(
        Page<ChatSummaryDto> chats
) {
}