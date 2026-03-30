package am.chat_service.exception;

import java.util.Collection;

public class DuplicateUserInChatException extends RuntimeException {

    public DuplicateUserInChatException(Collection<Long> duplicateUserIds) {
        super(STR."Duplicate users in chat request: \{duplicateUserIds}");
    }
}

