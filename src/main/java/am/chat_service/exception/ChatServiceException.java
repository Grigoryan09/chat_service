package am.chat_service.exception;

import lombok.Getter;

@Getter
public class ChatServiceException extends RuntimeException {
    private final String errorCode;

    public ChatServiceException(String message) {
        super(message);
        this.errorCode = "CHAT_SERVICE_ERROR";
    }

    public ChatServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CHAT_SERVICE_ERROR";
    }

    public ChatServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ChatServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}

