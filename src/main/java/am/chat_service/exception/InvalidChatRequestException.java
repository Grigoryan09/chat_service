package am.chat_service.exception;

import lombok.Getter;

@Getter
public class InvalidChatRequestException extends RuntimeException {
    private final String fieldName;
    private final String reason;

    public InvalidChatRequestException(String message) {
        super(message);
        this.fieldName = null;
        this.reason = null;
    }
}


