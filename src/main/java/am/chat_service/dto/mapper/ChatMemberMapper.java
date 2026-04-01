package am.chat_service.dto.mapper;

import am.chat_service.dto.ChatMemberDto;
import am.chat_service.model.ChatMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chat", ignore = true)
    ChatMember toEntity(ChatMemberDto chatMemberDto);

    ChatMemberDto toDto(ChatMember chatMember);

    List<ChatMemberDto> toDtoList(List<ChatMember> chatMembers);
}
