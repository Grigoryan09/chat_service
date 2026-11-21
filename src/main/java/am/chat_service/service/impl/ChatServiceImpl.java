package am.chat_service.service.impl;

import am.chat_service.cache.UserChatsCacheService;
import am.chat_service.dto.ChatDetailDto;
import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.ChatSummaryDto;
import am.chat_service.dto.UserChatsDto;
import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.DuplicateUserInChatException;
import am.chat_service.mapper.ChatMapper;
import am.chat_service.mapper.ChatMemberMapper;
import am.chat_service.mapper.ChatMessageMapper;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.ChatMessage;
import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import am.chat_service.model.enums.MessageStatus;
import am.chat_service.repository.ChatMessageRepository;
import am.chat_service.repository.ChatRepository;
import am.chat_service.service.ChatMemberService;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventPublisher chatEventPublisher;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMapper chatMapper;
    private final ChatMemberMapper chatMemberMapper;
    private final ChatMemberService chatMemberService;
    private final UserChatsCacheService userChatsCacheService;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChatServiceImpl that = (ChatServiceImpl) o;
        return Objects.equals(chatRepository, that.chatRepository) && Objects.equals(chatMessageRepository, that.chatMessageRepository) && Objects.equals(chatEventPublisher, that.chatEventPublisher) && Objects.equals(chatMessageMapper, that.chatMessageMapper) && Objects.equals(chatMapper, that.chatMapper) && Objects.equals(chatMemberMapper, that.chatMemberMapper) && Objects.equals(chatMemberService, that.chatMemberService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatRepository, chatMessageRepository, chatEventPublisher, chatMessageMapper, chatMapper, chatMemberMapper, chatMemberService);
    }

    @Override
    @Transactional
    public ChatDetailDto createChatFromExternal(CreateChatRequest request) {
        validateUsers(request.userIds());

        if (request.orderId() != null) {
            Optional<Chat> existing = chatRepository.findByOrderId(request.orderId());
            if (existing.isPresent()) {
                log.info("Chat {} already exists for order {}, returning it instead of creating a duplicate",
                        existing.get().getId(), request.orderId());
                return describe(existing.get(), "Chat already exists for this order");
            }
        }

        ChatType type = resolveChatType(request);
        Chat chat = createChatEntity(request, type);
        Chat savedChat = chatRepository.save(chat);

        ChatMessage firstMessage = createFirstMessage(savedChat);
        ChatMessage savedFirstMessage = chatMessageRepository.save(firstMessage);
        savedChat.setLastActivity(savedFirstMessage.getCreatedDate());
        chatRepository.save(savedChat);

        ChatMessageDto savedMsg = chatMessageMapper.toDto(savedFirstMessage);

        publishEvents(savedChat, request.userIds(), type, savedMsg);

        return describe(savedChat, "Chat created successfully");
    }

    private ChatDetailDto describe(Chat chat, String message) {
        return chatMapper.toDto(chat)
                .toBuilder()
                .status("SUCCESS")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public void changeChatStatus(ChangeChatStatusRequest request) {
        Chat chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new ChatNotFoundException(
                        "Chat with id %d not found".formatted(request.chatId())
                ));
        chat.setChatStatus(request.chatStatus());
        chatRepository.save(chat);
        chatEventPublisher.publishChatStatusChanged(chat.getId());
    }

    @Override
    public ChatDetailDto findById(long chatId) {
        return chatMapper.toDto(chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with id %d not found".formatted(chatId)
                )));
    }


    @Override
    @Transactional(readOnly = true)
    public ChatDetailDto getChatDetail(Long chatId, Pageable pageable) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(
                        "Chat with id %d not found".formatted(chatId)
                ));

        Page<ChatMessageDto> messages = chatMessageRepository
                .findByChatId(chatId, pageable)
                .map(chatMessageMapper::toDto);


        return ChatDetailDto.builder()
                .id(chat.getId())
                .chatType(chat.getChatType())
                .chatStatus(chat.getChatStatus())
                .lastActivity(chat.getLastActivity())
                .members(chatMemberMapper.toDtoList(chat.getChatMembers()))
                .messages(messages)
                .build();
    }

    private static final int CACHE_PAGE_SIZE = 10;

    @Override
    @Transactional(readOnly = true)
    public UserChatsDto getUserChats(Long userId, Pageable pageable) {
        if (!isCacheable(pageable)) {
            return loadUserChats(userId, pageable);
        }
        return userChatsCacheService.get(userId)
                .map(cached -> new UserChatsDto(
                        new PageImpl<>(
                                cached.chats(),
                                pageable,
                                cached.totalElements()
                        )
                ))
                .orElseGet(() -> loadUserChats(userId, pageable));
    }

    @Override
    @Transactional
    public void deleteChat(long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(
                        "Chat with id %d not found".formatted(chatId)
                ));

        chat.setChatStatus(ChatStatus.ARCHIVED);
        chatRepository.save(chat);

        chatEventPublisher.publishChatArchived(chatId);
    }

    private boolean isCacheable(Pageable pageable) {
        return pageable.isPaged()
                && pageable.getPageNumber() == 0
                && pageable.getPageSize() == CACHE_PAGE_SIZE;
    }

    private UserChatsDto loadUserChats(Long userId, Pageable pageable) {
        Page<ChatSummaryDto> chats = chatRepository
                .findChatsByUserId(userId, pageable)
                .map(chatMapper::toSummary);
        if (isCacheable(pageable)) {
            userChatsCacheService.put(
                    userId,
                    chats.getContent(),
                    chats.getTotalElements()
            );
        }
        return new UserChatsDto(chats);
    }

    private void validateUsers(List<Long> userIds) {
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (!seen.add(userId)) {
                duplicates.add(userId);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new DuplicateUserInChatException(
                    "Duplicate user IDs found in the request: %s".formatted(duplicates)
            );
        }
    }

    private ChatType resolveChatType(CreateChatRequest request) {
        return request.chatType() != null
                ? request.chatType()
                : (request.userIds().size() == 2 ? ChatType.ONE_TO_ONE : ChatType.GROUP);
    }

    private Chat createChatEntity(CreateChatRequest request, ChatType type) {
        Chat chat = new Chat();
        chat.setOrderId(request.orderId());
        chat.setCreatedDateTime(LocalDateTime.now());
        chat.setLastActivity(LocalDateTime.now());
        chat.setChatType(type);
        chat.setChatStatus(ChatStatus.ACTIVE);

        List<ChatMember> members = chatMemberService.getChatMembers(request, chat);
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







