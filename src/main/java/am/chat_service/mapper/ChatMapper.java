package am.chat_service.mapper;

import am.chat_service.dto.ChatDetailDto;
import am.chat_service.dto.ChatSummaryDto;
import am.chat_service.model.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {ChatMemberMapper.class, ChatMessageMapper.class})
public interface ChatMapper {

    @Mapping(source = "chatMembers", target = "members")
    @Mapping(target = "messages", ignore = true)
    ChatDetailDto toDto(Chat chat);

    @Mapping(source = "chatMembers", target = "members")
    ChatSummaryDto toSummary(Chat chat);
}