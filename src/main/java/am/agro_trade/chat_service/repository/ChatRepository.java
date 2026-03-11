package am.agro_trade.chat_service.repository;

import am.agro_trade.chat_service.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
