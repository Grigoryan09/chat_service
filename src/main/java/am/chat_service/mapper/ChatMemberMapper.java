package am.chat_service.mapper;

import am.chat_service.dto.ChatMemberDto;
import am.chat_service.model.ChatMember;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMemberMapper {

    ChatMemberDto toDto(ChatMember chatMember);

    List<ChatMemberDto> toDtoList(List<ChatMember> chatMembers);
}
