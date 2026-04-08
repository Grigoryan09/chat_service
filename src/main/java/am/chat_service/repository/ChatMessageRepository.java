package am.chat_service.repository;

import am.chat_service.model.ChatMessage;
import am.chat_service.model.enums.MessageStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm" +
            " WHERE cm.chat.id = :chatId" +
            " AND cm.chatMember.id = :memberId")
    List<ChatMessage> findByChatIdAndChatMemberId(@Param("chatId") long chatId, @Param("memberId") long memberId, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
                UPDATE chat_message
                SET status = :status
                WHERE chat_id = :chatId
                  AND chat_member_id IN (
                      SELECT cm.id FROM chat_member cm WHERE cm.user_id != :userId
                  )
                  AND status != :status
                RETURNING id
            """, nativeQuery = true)
    List<Long> markMessagesAsReadAndReturnIds(Long chatId, Long userId, String status);

    Page<ChatMessage> findByChatId(long chatId, Pageable pageable);
}


