package am.agro_trade.chat_service.dto.mapper;

import am.agro_trade.chat_service.dto.response.ChatDto;
import am.agro_trade.chat_service.model.Chat;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    ChatDto toDto(Chat chat);

    List<ChatDto> toDtoList(List<Chat> chats);
}
