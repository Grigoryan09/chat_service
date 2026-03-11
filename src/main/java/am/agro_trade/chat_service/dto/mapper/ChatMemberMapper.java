package am.agro_trade.chat_service.dto.mapper;

import am.agro_trade.chat_service.dto.response.ChatMemberDto;
import am.agro_trade.chat_service.model.ChatMember;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMemberMapper {

    ChatMemberDto toDto(ChatMember chatMember);

    List<ChatMemberDto> toDtoList(List<ChatMember> chatMembers);
}
