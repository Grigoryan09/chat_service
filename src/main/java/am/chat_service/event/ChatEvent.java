package am.chat_service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class ChatEvent extends ApplicationEvent {

    private final String eventType;
    private final Long chatId;
    private final Map<String, Object> payload;

    public ChatEvent(Object source, String eventType, Long chatId, Map<String, Object> payload) {
        super(source);
        this.eventType = eventType;
        this.chatId = chatId;
        this.payload = payload;
    }
}

