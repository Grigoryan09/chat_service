package am.chat_service.service;

import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.dto.response.ChatMessageDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatMessageService {

    ChatMessageDto sendMessage(SendMessageRequest sendMessageRequest);

    void updateChatMessage(UpdateMessageRequest updateMessageReadRequest);

    void deleteChatMessage(long chatMessageId);

    List<ChatMessageDto> getChatMessagesByMemberId(long chatId, long memberId, Pageable pageable);
}
