package am.chat_service.repository;

import am.chat_service.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    Optional<ChatMember> findById(long memberId);
}
