package am.chat_service.cache;

import am.chat_service.event.ChatEvent;
import am.chat_service.service.ChatMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCacheInvalidationListener {

    private final UserChatsCacheService cacheService;
    private final ChatMemberService chatMemberService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(ChatEvent event) {

        try {
            switch (event.getEventType()) {

                case CHAT_OPENED ->
                        evictUserIds(event.getPayload().get("userIds"));

                case NEW_MESSAGE,
                     MESSAGE_DELETED,
                     CHAT_ARCHIVED,
                     CHAT_STATUS_CHANGED -> evictChatMembers(event.getChatId());

                default -> {
                }
            }

        } catch (Exception e) {
            log.error(
                    "Failed to invalidate chat cache for event {}",
                    event.getEventType(),
                    e
            );
        }
    }

    private void evictUserIds(Object value) {
        if (!(value instanceof Collection<?> userIds)) {
            return;
        }

        userIds.stream()
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .distinct()
                .forEach(cacheService::evict);
    }

    private void evictChatMembers(Long chatId) {
        if (chatId == null) {
            return;
        }

        List<Long> userIds =
                chatMemberService.getMembersByChatId(chatId);

        userIds.stream()
                .distinct()
                .forEach(cacheService::evict);
    }
}
