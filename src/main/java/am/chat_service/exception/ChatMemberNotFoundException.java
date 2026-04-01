package am.chat_service.exception;

public class ChatMemberNotFoundException extends RuntimeException {

    public ChatMemberNotFoundException(String message) {
        super(message);
    }
}

