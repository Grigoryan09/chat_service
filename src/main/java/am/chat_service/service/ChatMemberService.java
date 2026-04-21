package am.chat_service.service;


import am.chat_service.dto.ChatMemberDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;

import java.util.List;

/**
 * Service for chat member lookup and member creation logic.
 */
public interface ChatMemberService {

    /**
     * Finds a chat member by its identifier.
     *
     * @param memberId member identifier
     * @return member details
     */
    ChatMemberDto findById(long memberId);

    /**
     * Builds chat member entities for a newly created chat.
     *
     * @param request chat creation payload
     * @param chat persisted chat entity
     * @return list of chat member entities
     */
    List<ChatMember> getChatMembers(CreateChatRequest request, Chat chat);

    /**
     * Returns the identifiers of all members belonging to the given chat.
     *
     * @param chatId chat identifier
     * @return member identifiers for the chat
     */
    List<Long> getMembersByChatId(Long chatId);
}
