package am.chat_service.service.impl;

import am.chat_service.dto.mapper.ChatMapper;
import am.chat_service.dto.mapper.ChatMessageMapper;
import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatFromExternalRequest;
import am.chat_service.dto.response.ChatDto;
import am.chat_service.dto.response.ChatMessageDto;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.DuplicateUserInChatException;
import am.chat_service.exception.InvalidChatRequestException;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.ChatMessage;
import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import am.chat_service.repository.ChatRepository;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatEventPublisher chatEventPublisher;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMapper chatMapper;


    @Override
    @Transactional
    public Long createChatFromExternal(CreateChatFromExternalRequest request) {

        Set<Long> uniqueIds = new HashSet<>(request.getUserIds());
        if (uniqueIds.size() != request.getUserIds().size()) {
            throw new DuplicateUserInChatException(request.getUserIds());
        }
        ChatType type = request.getChatType() != null
                ? request.getChatType()
                : (request.getUserIds().size() == 2 ? ChatType.ONE_TO_ONE : ChatType.GROUP);
        Chat chat = new Chat();
        chat.setCreatedDateTime(LocalDateTime.now());
        chat.setLastActivity(LocalDateTime.now());
        chat.setChatType(type);
        chat.setChatStatus(ChatStatus.ACTIVE);
        List<ChatMember> members = request.getUserIds().stream()
                .map(userId -> {
                    ChatMember m = new ChatMember();
                    m.setChat(chat);
                    m.setUserId(userId);
                    return m;
                })
                .toList();
        chat.setChatMembers(members);
        Chat savedChat = chatRepository.save(chat);
        ChatMessage firstMessage = new ChatMessage();
        firstMessage.setChat(chat);
        firstMessage.setMessage("Chat created");
        firstMessage.setCreatedDate(LocalDateTime.now());
        firstMessage.setUpdatedDate(LocalDateTime.now());
        chat.setChatMessages(List.of(firstMessage));
        ChatMessageDto savedMsg = chatMessageMapper.toDto(firstMessage);
        chatEventPublisher.publishChatOpened(
                savedChat.getId(),
                request.getUserIds(),
                Map.of(
                        "chatId", savedChat.getId(),
                        "type", type.name(),
                        "name", STR."Chat \{savedChat.getId()}",
                        "firstMessage", savedMsg
                )
        );

        chatEventPublisher.publishNewMessage(savedChat.getId(), savedMsg);

        return savedChat.getId();
    }

    @Override
    public void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest) {
        if (updateChatStatusRequest == null) {
            throw new InvalidChatRequestException("ChangeChatStatusRequest cannot be null");
        }

        if (updateChatStatusRequest.chatId() <= 0) {
            throw new InvalidChatRequestException("ID must be positive" + updateChatStatusRequest.chatId());
        }

        if (updateChatStatusRequest.chatStatus() == null) {
            throw new InvalidChatRequestException("Status cannot be null" + updateChatStatusRequest.chatStatus());
        }

        Chat chat = chatRepository.findById(updateChatStatusRequest.chatId())
                .orElseThrow(() -> new ChatNotFoundException(updateChatStatusRequest.chatId()));

        chat.setChatStatus(updateChatStatusRequest.chatStatus());
        chatRepository.save(chat);
    }

    @Override
    public ChatDto findById(long chatId) {
        if (chatId <= 0) {
            throw new InvalidChatRequestException("Chat ID must be positive" + chatId);
        }

        return chatMapper.toDto(chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId)));
    }
}
