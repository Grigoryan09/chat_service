package am.agro_trade.chat_service.service;

import am.agro_trade.chat_service.dto.request.SendMessageRequest;
import am.agro_trade.chat_service.dto.request.UpdateMessageRequest;
import am.agro_trade.chat_service.dto.response.ChatMessageDto;

import java.awt.print.Pageable;
import java.util.List;

public interface ChatMessageService {

    void save(SendMessageRequest sendMessageRequest);

    void updateChatMessage(UpdateMessageRequest updateMessageReadRequest);

    void deleteChatMessage(long chatMessageId);

    List<ChatMessageDto> getChatMessagesByMemberId(long chatId , long memberId, Pageable pageable);
}
