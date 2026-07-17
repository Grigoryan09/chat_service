package am.chat_service.service;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatMessageRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for sending, updating and retrieving chat messages.
 */
public interface ChatMessageService {

    /**
     * Sends a new message to a chat.
     *
     * @param sendMessageRequest message payload
     * @return sent message details
     */
    ChatMessageDto sendMessage(SendMessageRequest sendMessageRequest);

    /**
     * Sends a system message (e.g. a generated document link) to a chat on behalf
     * of the member identified by the given user id.
     *
     * @param chatId       chat identifier
     * @param senderUserId user id of the member the message is attributed to
     * @param message      message text
     * @return sent message details
     */
    ChatMessageDto sendDocumentMessage(long chatId, long senderUserId, String message);

    /**
     * Marks a message as delivered.
     *
     * @param messageId message identifier
     */
    void markAsDelivered(Long messageId);

    /**
     * Marks chat messages as read for a specific user.
     *
     * @param chatId chat identifier
     * @param userId user identifier
     * @param status target message status
     * @return identifiers of messages that were updated
     */
    List<Long> markAsRead(Long chatId, Long userId, String status);

    /**
     * Finds a message by its identifier.
     *
     * @param messageId message identifier
     * @return message details
     */
    ChatMessageDto getMessageById(long messageId);

    /**
     * Updates an existing chat message.
     *
     * @param updateMessageReadRequest message update payload
     * @return updated message details
     */
    ChatMessageDto updateChatMessage(UpdateMessageRequest updateMessageReadRequest);

    /**
     * Deletes a chat message by identifier.
     *
     * @param chatMessageId message identifier
     */
    void deleteChatMessage(long chatMessageId);

    /**
     * Returns paginated chat messages available to a member.
     *
     * @param request  message query payload
     * @param pageable pagination settings
     * @return paginated message details
     */
    List<ChatMessageDto> getChatMessagesByMemberId(ChatMessageRequest request, Pageable pageable);
}
