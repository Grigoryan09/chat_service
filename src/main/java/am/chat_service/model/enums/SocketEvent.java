package am.chat_service.model.enums;

public enum SocketEvent {

    SEND_MESSAGE("send_message"),
    JOIN_CHAT("join_chat"),
    LEAVE_CHAT("leave_chat"),
    CHAT_OPENED_ACK("chat_opened_ack"),
    MESSAGES_READ("messages_read"),
    MESSAGE_DELIVERED("message_delivered"),
    MESSAGE_NOTIFICATION("new_message_notification");

    private final String value;

    SocketEvent(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}