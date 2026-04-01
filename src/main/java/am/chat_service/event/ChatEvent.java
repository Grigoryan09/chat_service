package am.chat_service.event;

import am.chat_service.model.enums.EventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class ChatEvent extends ApplicationEvent {

    private final EventType eventType;
    private final Long chatId;
    private final Map<String, Object> payload;

    public ChatEvent(Object source, EventType eventType, Long chatId, Map<String, Object> payload) {
        super(source);
        this.eventType = eventType;
        this.chatId = chatId;
        this.payload = payload;
    }
}

