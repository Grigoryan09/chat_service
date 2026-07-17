package am.chat_service.endpoint.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.request.EditMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.dto.response.ChatResponse;
import am.chat_service.dto.response.UserChatsResponse;
import am.chat_service.endpoint.CreatedChatV1API;
import am.chat_service.service.ChatMessageService;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CreatedChatEndpoint implements CreatedChatV1API {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;

    @Override
    public ChatResponse createChatFromExternal(CreateChatRequest request) {
        return new ChatResponse(chatService.createChatFromExternal(request));
    }

    @Override
    public ChatResponse getChatDetail(Long id, Pageable pageable) {
        return new ChatResponse(chatService.getChatDetail(id, pageable));
    }

    @Override
    public UserChatsResponse getUserChats(Long userId, Pageable pageable) {
        return new UserChatsResponse(chatService.getUserChats(userId, pageable));
    }

    @Override
    public ChatMessageDto updateMessage(Long messageId, EditMessageRequest request) {
        return chatMessageService.updateChatMessage(
                new UpdateMessageRequest(messageId, request.message())
        );
    }

    @Override
    public void deleteMessage(Long messageId) {
        chatMessageService.deleteChatMessage(messageId);
    }

    @Override
    public void deleteChat(Long id) {
        chatService.deleteChat(id);
    }
}