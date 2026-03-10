package am.agro_trade.chat_service.dto.mapper;

import am.agro_trade.chat_service.dto.response.ChatMessageDto;
import am.agro_trade.chat_service.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "chatMember.userId", target = "userId")
    ChatMessageDto toDto(ChatMessage message);

    List<ChatMessageDto> toDtoList(List<ChatMessage> messages);
}
