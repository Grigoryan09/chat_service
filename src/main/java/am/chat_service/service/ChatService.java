package am.chat_service.service;

import am.chat_service.dto.ChatDetailDto;
import am.chat_service.dto.UserChatsDto;
import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service for chat lifecycle operations and chat detail retrieval.
 */
public interface ChatService {

    /**
     * Creates a new chat using an externally provided request payload.
     *
     * @param request chat creation payload
     * @return created chat details
     */
    ChatDetailDto createChatFromExternal(CreateChatRequest request);

    /**
     * Updates the status of an existing chat.
     *
     * @param updateChatStatusRequest status update payload
     */
    void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest);

    /**
     * Finds a chat by its identifier.
     *
     * @param chatId chat identifier
     * @return chat details
     */
    ChatDetailDto findById(long chatId);

    /**
     * Returns chat details together with a paginated slice of messages.
     *
     * @param chatId   chat identifier
     * @param pageable pagination settings for messages
     * @return chat details with messages page
     */
    ChatDetailDto getChatDetail(Long chatId, Pageable pageable);

    /**
     * Returns a paginated list of chats the user participates in,
     * most recently active first, excluding archived chats.
     *
     * @param userId   user identifier
     * @param pageable pagination settings
     * @return DTO holding a page of chat summaries
     */
    UserChatsDto getUserChats(Long userId, Pageable pageable);

    /**
     * Soft-deletes a chat by moving it to the archived state and
     * notifying participants.
     *
     * @param chatId chat identifier
     */
    void deleteChat(long chatId);
}
