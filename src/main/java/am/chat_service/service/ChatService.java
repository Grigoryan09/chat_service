package am.chat_service.service;

import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatFromExternalRequest;
import am.chat_service.dto.response.ChatDto;

public interface ChatService {

    Long createChatFromExternal(CreateChatFromExternalRequest request);

    void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest);

    ChatDto findById(long chatId);
}
