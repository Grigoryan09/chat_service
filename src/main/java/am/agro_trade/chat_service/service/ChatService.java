package am.agro_trade.chat_service.service;

import am.agro_trade.chat_service.model.Chat;

import java.util.Optional;

public interface ChatService {

    void saveChat(Chat chat);

    Optional<Chat> findByChatId(long chatId);

    void deleteChatId(long chatId);
}
