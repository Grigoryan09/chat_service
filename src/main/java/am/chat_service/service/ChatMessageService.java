package am.chat_service.service;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatMessageRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.model.enums.MessageStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatMessageService {

    ChatMessageDto sendMessage(SendMessageRequest sendMessageRequest);

    void markAsDelivered(Long messageId);

    List<Long> markAsRead(Long chatId, Long userId, MessageStatus status);

    ChatMessageDto getMessageById(long messageId);

    void updateChatMessage(UpdateMessageRequest updateMessageReadRequest);

    void deleteChatMessage(long chatMessageId);

    List<ChatMessageDto> getChatMessagesByMemberId(ChatMessageRequest request, Pageable pageable);
}
