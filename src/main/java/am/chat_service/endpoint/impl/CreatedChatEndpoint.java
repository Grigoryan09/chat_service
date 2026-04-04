package am.chat_service.endpoint.impl;

import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.response.ChatResponse;
import am.chat_service.endpoint.CreatedChatV1API;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CreatedChatEndpoint implements CreatedChatV1API {

    private final ChatService chatService;

    @Override
    public ChatResponse createChatFromExternal(CreateChatRequest request) {
        return new ChatResponse(chatService.createChatFromExternal(request));
    }

    @Override
    public ChatResponse getChatDetail(Long id, Pageable pageable) {
        return new ChatResponse(chatService.getChatDetail(id, pageable));
    }


}
