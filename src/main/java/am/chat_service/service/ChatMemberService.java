package am.chat_service.service;


import am.chat_service.dto.ChatMemberDto;

public interface ChatMemberService {

    ChatMemberDto findById(long memberId);
}
