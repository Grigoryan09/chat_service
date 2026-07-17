package am.chat_service.repository;

import am.chat_service.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findById(Long id);

    @Query(value = "SELECT cm.chat FROM ChatMember cm"
            + " WHERE cm.userId = :userId"
            + " AND cm.chat.chatStatus <> am.chat_service.model.enums.ChatStatus.ARCHIVED"
            + " ORDER BY cm.chat.lastActivity DESC",
            countQuery = "SELECT count(cm) FROM ChatMember cm"
                    + " WHERE cm.userId = :userId"
                    + " AND cm.chat.chatStatus <> am.chat_service.model.enums.ChatStatus.ARCHIVED")
    Page<Chat> findChatsByUserId(@Param("userId") Long userId, Pageable pageable);
}
