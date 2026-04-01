package am.chat_service.service.impl;

import am.chat_service.dto.mapper.ChatMemberMapper;
import am.chat_service.dto.ChatMemberDto;
import am.chat_service.exception.ChatMemberNotFoundException;
import am.chat_service.repository.ChatMemberRepository;
import am.chat_service.service.ChatMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMemberServiceImpl implements ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatMemberMapper chatMemberMapper;

    @Override
    public ChatMemberDto findById(long memberId) {
        return chatMemberMapper.toDto(
                chatMemberRepository.findOptionalById(memberId)
                        .orElseThrow(() -> new ChatMemberNotFoundException("Chat member not found with id: " + memberId)));
    }
}
