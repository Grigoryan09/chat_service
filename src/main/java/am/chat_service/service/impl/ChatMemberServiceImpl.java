package am.chat_service.service.impl;

import am.chat_service.dto.ChatMemberDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.exception.ChatMemberNotFoundException;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.mapper.ChatMemberMapper;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.repository.ChatMemberRepository;
import am.chat_service.service.ChatMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMemberServiceImpl implements ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatMemberMapper chatMemberMapper;

    @Override
    public ChatMemberDto findById(long memberId) {
        return chatMemberMapper.toDto(
                chatMemberRepository.findOptionalById(memberId)
                        .orElseThrow(() -> new ChatMemberNotFoundException(
                                "Chat member not found with id: %d".formatted(memberId)
                        )));
    }

    public List<ChatMember> getChatMembers(CreateChatRequest request, Chat chat) {
        return request.userIds().stream()
                .map(userId -> {
                    ChatMember m = new ChatMember();
                    m.setChat(chat);
                    m.setUserId(userId);
                    return m;
                })
                .toList();
    }

    public List<Long> getMembersByChatId(Long chatId) {
        if (chatId != null) {
            return chatMemberRepository.findUserIdsByChatId(chatId);
        }
        throw new ChatNotFoundException("Chat not found");
    }
}
