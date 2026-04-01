package am.chat_service.service;

import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.ChatDto;

public interface ChatService {

    ChatDto createChatFromExternal(CreateChatRequest request);

    void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest);

    ChatDto findById(long chatId);
}
