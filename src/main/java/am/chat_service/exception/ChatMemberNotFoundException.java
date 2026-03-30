package am.chat_service.exception;

public class ChatMemberNotFoundException extends RuntimeException {

    public ChatMemberNotFoundException(Long memberId) {
        super("Chat member not found with id: " + memberId);
    }
}

