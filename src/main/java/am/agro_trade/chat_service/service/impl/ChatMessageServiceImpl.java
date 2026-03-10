package am.agro_trade.chat_service.service.impl;

import am.agro_trade.chat_service.model.ChatMessage;
import am.agro_trade.chat_service.repository.ChatMessageRepository;
import am.agro_trade.chat_service.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void save(ChatMessage chatMessage) {
        chatMessageRepository.save(chatMessage);
    }

    @Override
    public void deleteChatMessage(long chatMessageId) {
        chatMessageRepository.deleteById(chatMessageId);
    }
}
