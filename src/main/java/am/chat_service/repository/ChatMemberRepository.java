package am.chat_service.repository;

import am.chat_service.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    ChatMember findById(long memberId);

    Optional<ChatMember> findOptionalById(long memberId);

    @Query("SELECT cm.userId FROM ChatMember cm WHERE cm.chat.id = :chatId")
    Optional<List<Long>> findUserIdsByChatId(@Param("chatId") Long chatId);
}
