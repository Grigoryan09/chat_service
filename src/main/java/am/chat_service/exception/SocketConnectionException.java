package am.chat_service.exception;

public class SocketConnectionException extends ChatServiceException {

    public SocketConnectionException(String message, Throwable cause) {
        super(message, "SOCKET_CONNECTION_ERROR", cause);
    }
}