package am.agro_trade.chat_service.service.impl;

import am.agro_trade.chat_service.dto.request.SendMessageRequest;
import am.agro_trade.chat_service.dto.request.UpdateMessageRequest;
import am.agro_trade.chat_service.dto.response.ChatMessageDto;
import am.agro_trade.chat_service.model.ChatMessage;
import am.agro_trade.chat_service.repository.ChatMessageRepository;
import am.agro_trade.chat_service.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;


    @Override
    public void save(SendMessageRequest sendMessageRequest) {

    }

    @Override
    public void updateChatMessage(UpdateMessageRequest updateMessageReadRequest) {

    }

    @Override
    public void deleteChatMessage(long chatMessageId) {
        chatMessageRepository.deleteById(chatMessageId);
    }

    @Override
    public List<ChatMessageDto> getChatMessagesByMemberId(long chatId, long memberId, Pageable pageable) {
        return List.of();
    }

}
