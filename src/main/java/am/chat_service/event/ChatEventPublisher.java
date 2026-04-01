package am.chat_service.event;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.model.enums.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishNewMessage(Long chatId, ChatMessageDto messageDto) {
        eventPublisher.publishEvent(new ChatEvent(
                this,
                EventType.NEW_MESSAGE,
                chatId,
                Map.of("message", messageDto)
        ));
    }

    public void publishChatOpened(Long chatId, List<Long> userIds, Map<String, Object> chatData) {
        eventPublisher.publishEvent(new ChatEvent(
                this,
                EventType.CHAT_OPENED,
                chatId,
                Map.of("userIds", userIds, "data", chatData)
        ));
    }

    public void publishUserJoined(Long chatId, Long userId) {
        eventPublisher.publishEvent(new ChatEvent(
                this,
                EventType.USER_JOINED,
                chatId,
                Map.of("userId", userId)
        ));
    }

    public void publishUserLeft(Long chatId, Long userId) {
        eventPublisher.publishEvent(new ChatEvent(
                this,
                EventType.USER_LEFT,
                chatId,
                Map.of("userId", userId)
        ));
    }
}

