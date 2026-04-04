package am.chat_service.mapper;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "userId", source = "chatMember.userId")
    ChatMessageDto toDto(ChatMessage message);

    List<ChatMessageDto> toDtoList(List<ChatMessage> chatMessages);


}
