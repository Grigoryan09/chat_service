package am.chat_service.dto.mapper;

import am.chat_service.dto.response.ChatDto;
import am.chat_service.model.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "chatMembers", ignore = true)
    @Mapping(target = "chatMessages", ignore = true)
    @Mapping(target = "lastActivity", ignore = true)
    Chat toEntity(ChatDto chatDto);

    @Mapping(source = "chatMembers", target = "members")
    @Mapping(source = "chatMessages", target = "messages")
    ChatDto toDto(Chat chat);

    List<ChatDto> toDtoList(List<Chat> chats);
}
