package am.chat_service.cache;

import am.chat_service.dto.ChatSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserChatsCacheService {

    private static final String KEY_PREFIX =
            "chat-service:user-chats:v1:";

    private static final Duration TTL =
            Duration.ofMinutes(5);

    private static final TypeReference<CachedUserChats> CACHE_TYPE =
            new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public Optional<CachedUserChats> get(Long userId) {
        String key = buildKey(userId);

        try {
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(
                    jsonMapper.readValue(value, CACHE_TYPE)
            );

        } catch (Exception e) {
            log.warn("Failed to read chats cache for user {}", userId, e);

            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
                log.warn("Failed to remove invalid cache for user {}", userId);
            }

            return Optional.empty();
        }
    }

    public void put(
            Long userId,
            List<ChatSummaryDto> chats,
            long totalElements
    ) {
        try {
            CachedUserChats cached = new CachedUserChats(
                    List.copyOf(chats),
                    totalElements
            );

            String value = jsonMapper.writeValueAsString(cached);

            redisTemplate.opsForValue().set(
                    buildKey(userId),
                    value,
                    TTL
            );

        } catch (Exception e) {
            log.warn("Failed to save chats cache for user {}", userId, e);
        }
    }

    public void evict(Long userId) {
        try {
            redisTemplate.delete(buildKey(userId));
        } catch (Exception e) {
            log.warn("Failed to evict chats cache for user {}", userId, e);
        }
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId + ":top10";
    }

    public record CachedUserChats(
            List<ChatSummaryDto> chats,
            long totalElements
    ) {}
}
