package am.chat_service.service;


import am.chat_service.dto.ChatMemberDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;

import java.util.List;

public interface ChatMemberService {

    ChatMemberDto findById(long memberId);

    List<ChatMember> getChatMembers(CreateChatRequest request, Chat chat);
}
