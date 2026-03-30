package am.chat_service.service;


import am.chat_service.dto.response.ChatMemberDto;

public interface ChatMemberService {

    ChatMemberDto findById(long memberId);
}
