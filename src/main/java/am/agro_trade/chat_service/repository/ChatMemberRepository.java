package am.agro_trade.chat_service.repository;

import am.agro_trade.chat_service.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
}
