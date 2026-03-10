package am.agro_trade.chat_service.service.impl;
import am.agro_trade.chat_service.model.Chat;
import am.agro_trade.chat_service.repository.ChatRepository;
import am.agro_trade.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    @Override
    public void saveChat(Chat chat) {
        chatRepository.save(chat);
    }

    @Override
    public Optional<Chat> findByChatId(long chatId) {
        return Optional.empty();
    }

    @Override
    public void deleteChatId(long chatId) {
        chatRepository.deleteById(chatId);
    }
}
