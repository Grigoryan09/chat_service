package am.chat_service.service;

import am.chat_service.dto.ChatDetailDto;
import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatRequest;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    ChatDetailDto createChatFromExternal(CreateChatRequest request);

    void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest);

    ChatDetailDto findById(long chatId);

    ChatDetailDto getChatDetail(Long chatId, Pageable pageable);
}
