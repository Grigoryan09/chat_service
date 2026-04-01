package am.chat_service.service.impl;

import am.chat_service.dto.mapper.ChatMapper;
import am.chat_service.dto.mapper.ChatMessageMapper;
import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.ChatDto;
import am.chat_service.dto.ChatMessageDto;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.DuplicateUserInChatException;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.ChatMessage;
import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import am.chat_service.model.enums.MessageStatus;
import am.chat_service.repository.ChatRepository;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatEventPublisher chatEventPublisher;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMapper chatMapper;


    @Override
    @Transactional
    public ChatDto createChatFromExternal(CreateChatRequest request) {

        validateUsers(request.userIds());

        ChatType type = resolveChatType(request);

        Chat chat = createChatEntity(request, type);

        Chat savedChat = chatRepository.save(chat);

        ChatMessage firstMessage = createFirstMessage(savedChat);

        ChatMessageDto savedMsg = chatMessageMapper.toDto(firstMessage);

        publishEvents(savedChat, request.userIds(), type, savedMsg);

        return chatMapper.toDto(savedChat);
    }

    @Override
    public void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest) {
        Chat chat = chatRepository.findById(updateChatStatusRequest.chatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with id " + updateChatStatusRequest.chatId() + " not found"));

        chat.setChatStatus(updateChatStatusRequest.chatStatus());
        chatRepository.save(chat);
    }

    @Override
    public ChatDto findById(long chatId) {
        return chatMapper.toDto(chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with id " + chatId + " not found")));
    }

    private void validateUsers(List<Long> userIds) {
        Set<Long> uniqueIds = new HashSet<>(userIds);
        if (uniqueIds.size() != userIds.size()) {
            throw new DuplicateUserInChatException("Duplicate user IDs found in the request: " + userIds);
        }
    }

    private ChatType resolveChatType(CreateChatRequest request) {
        return request.chatType() != null
                ? request.chatType()
                : (request.userIds().size() == 2 ? ChatType.ONE_TO_ONE : ChatType.GROUP);
    }

    private Chat createChatEntity(CreateChatRequest request, ChatType type) {
        Chat chat = new Chat();
        chat.setCreatedDateTime(LocalDateTime.now());
        chat.setLastActivity(LocalDateTime.now());
        chat.setChatType(type);
        chat.setChatStatus(ChatStatus.ACTIVE);

        List<ChatMember> members = request.userIds().stream()
                .map(userId -> {
                    ChatMember m = new ChatMember();
                    m.setChat(chat);
                    m.setUserId(userId);
                    return m;
                })
                .toList();

        chat.setChatMembers(members);
        chat.setChatMessages(new ArrayList<>());
        return chat;
    }

    private ChatMessage createFirstMessage(Chat chat) {
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setMessage("Chat created");
        message.setStatus(MessageStatus.SENT);
        message.setCreatedDate(LocalDateTime.now());
        message.setUpdatedDate(LocalDateTime.now());

        chat.getChatMessages().add(message);

        return message;
    }

    private void publishEvents(Chat savedChat,
                               List<Long> userIds,
                               ChatType type,
                               ChatMessageDto savedMsg) {

        chatEventPublisher.publishChatOpened(
                savedChat.getId(),
                userIds,
                Map.of(
                        "chatId", savedChat.getId(),
                        "type", type.name(),
                        "name", "Chat " + savedChat.getId(),
                        "firstMessage", savedMsg
                )
        );

        chatEventPublisher.publishNewMessage(savedChat.getId(), savedMsg);
    }
}







