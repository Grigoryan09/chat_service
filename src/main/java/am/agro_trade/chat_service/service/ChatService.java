package am.agro_trade.chat_service.service;

import am.agro_trade.chat_service.dto.request.ChangeChatStatusRequest;
import am.agro_trade.chat_service.dto.request.CreateChatRequest;
import am.agro_trade.chat_service.dto.response.ChatDto;

public interface ChatService {

    long saveChat(CreateChatRequest createChatRequest);

    void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest);

    ChatDto findByChatId(long chatId);
}
