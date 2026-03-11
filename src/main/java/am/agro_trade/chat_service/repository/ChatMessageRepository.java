package am.agro_trade.chat_service.repository;

import am.agro_trade.chat_service.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
