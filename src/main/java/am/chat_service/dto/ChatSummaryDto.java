package am.chat_service.dto;

import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSummaryDto(

        Long id,
        ChatType chatType,
        ChatStatus chatStatus,
        LocalDateTime lastActivity,
        List<ChatMemberDto> members
) {
}