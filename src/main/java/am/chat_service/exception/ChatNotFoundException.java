package am.chat_service.exception;

import lombok.Getter;

@Getter
public class ChatNotFoundException extends RuntimeException {
    private final Long chatId;

    public ChatNotFoundException(Long chatId) {
        super("Chat not found with id: " + chatId);
        this.chatId = chatId;
    }

    public ChatNotFoundException(String message) {
        super(message);
        this.chatId = null;
    }

}

