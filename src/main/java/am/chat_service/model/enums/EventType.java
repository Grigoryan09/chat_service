package am.chat_service.model.enums;

public enum EventType {

    NEW_MESSAGE("new_message"),
    CHAT_OPENED("chat_opened"),
    USER_JOINED("user_joined"),
    USER_LEFT("user_left"),
    MESSAGE_UPDATED("message_updated"),
    MESSAGE_DELETED("message_deleted"),
    CHAT_ARCHIVED("chat_archived");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
