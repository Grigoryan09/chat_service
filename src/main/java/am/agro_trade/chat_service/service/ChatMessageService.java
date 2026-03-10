package am.agro_trade.chat_service.service;

import am.agro_trade.chat_service.model.ChatMessage;

public interface ChatMessageService {

    void save(ChatMessage chatMessage);

    void deleteChatMessage(long chatMessageId);
}
