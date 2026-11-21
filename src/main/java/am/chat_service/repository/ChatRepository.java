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

    Optional<Chat> findByOrderId(Long orderId);

    @Query(
            value = """
            SELECT c
            FROM Chat c
            WHERE c.chatStatus <> am.chat_service.model.enums.ChatStatus.ARCHIVED
              AND EXISTS (
                  SELECT 1
                  FROM ChatMember cm
                  WHERE cm.chat = c
                    AND cm.userId = :userId
              )
            ORDER BY c.lastActivity DESC, c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c)
            FROM Chat c
            WHERE c.chatStatus <> am.chat_service.model.enums.ChatStatus.ARCHIVED
              AND EXISTS (
                  SELECT 1
                  FROM ChatMember cm
                  WHERE cm.chat = c
                    AND cm.userId = :userId
              )
            """
    )
    Page<Chat> findChatsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
