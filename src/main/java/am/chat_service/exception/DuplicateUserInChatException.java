package am.chat_service.exception;

public class DuplicateUserInChatException extends RuntimeException {

    public DuplicateUserInChatException(String message) {
        super(message);
    }
}

