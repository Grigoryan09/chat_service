package am.chat_service.service.impl;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.ChatMessageRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatMemberNotFoundException;
import am.chat_service.exception.ChatMessageNotFoundException;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.InvalidChatRequestException;
import am.chat_service.mapper.ChatMessageMapper;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.ChatMessage;
import am.chat_service.model.enums.MessageStatus;
import am.chat_service.repository.ChatMemberRepository;
import am.chat_service.repository.ChatMessageRepository;
import am.chat_service.repository.ChatRepository;
import am.chat_service.service.ChatMessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatEventPublisher chatEventPublisher;


    @Override
    @Transactional
    public ChatMessageDto sendMessage(SendMessageRequest request) {

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new ChatNotFoundException(
                        "Chat not found with id: %d".formatted(request.getChatId())
                ));

        ChatMember member = chatMemberRepository.findOptionalById(request.getMemberId())
                .orElseThrow(() -> new ChatMemberNotFoundException(
                        "Chat member not found with id: %d".formatted(request.getMemberId())
                ));

        validateMemberBelongsToChat(member, chat);

        chat.setLastActivity(LocalDateTime.now());
        chatRepository.save(chat);

        ChatMessage chatMessage = createMessage(request, chat, member);
        ChatMessageDto messageDto = chatMessageMapper.toDto(
                chatMessageRepository.save(chatMessage)
        );

        chatEventPublisher.publishNewMessage(request.getChatId(), messageDto);

        return messageDto;
    }

    @Override
    @Transactional
    public ChatMessageDto sendDocumentMessage(long chatId, long senderUserId, String message) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(
                        "Chat not found with id: %d".formatted(chatId)
                ));

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, senderUserId)
                .orElseThrow(() -> new ChatMemberNotFoundException(
                        "Chat member not found in chat %d for user %d".formatted(chatId, senderUserId)
                ));

        chat.setLastActivity(LocalDateTime.now());
        chatRepository.save(chat);

        ChatMessage chatMessage = ChatMessage.builder()
                .chat(chat)
                .chatMember(member)
                .createdDate(LocalDateTime.now())
                .message(message)
                .status(MessageStatus.SENT)
                .build();

        ChatMessageDto messageDto = chatMessageMapper.toDto(
                chatMessageRepository.save(chatMessage)
        );

        chatEventPublisher.publishNewMessage(chatId, messageDto);

        return messageDto;
    }

    private void validateMemberBelongsToChat(ChatMember member, Chat chat) {
        if (!Objects.equals(member.getChat().getId(), chat.getId())) {
            throw new InvalidChatRequestException(
                    "Member %d does not belong to chat %d".formatted(
                            member.getId(), chat.getId()
                    )
            );
        }
    }

    private ChatMessage createMessage(SendMessageRequest request, Chat chat, ChatMember member) {
        return ChatMessage.builder()
                .chat(chat)
                .chatMember(member)
                .createdDate(LocalDateTime.now())
                .message(request.getMessage())
                .status(MessageStatus.SENT)
                .build();
    }

    @Override
    @Transactional
    public ChatMessageDto updateChatMessage(UpdateMessageRequest request) {

        ChatMessage message = chatMessageRepository.findById(request.messageId())
                .orElseThrow(()
                        -> new ChatMessageNotFoundException("Chat message not found with id:" + request.messageId()));
        message.setMessage(request.message());
        message.setUpdatedDate(LocalDateTime.now());

        ChatMessageDto messageDto = chatMessageMapper.toDto(chatMessageRepository.save(message));

        chatEventPublisher.publishMessageUpdated(message.getChat().getId(), messageDto);

        return messageDto;
    }

    @Override
    @Transactional
    public void deleteChatMessage(long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException(
                        "Chat message not found with id: " + messageId));

        Long chatId = message.getChat().getId();
        chatMessageRepository.delete(message);

        chatEventPublisher.publishMessageDeleted(chatId, messageId);
    }

    @Override
    public List<ChatMessageDto> getChatMessagesByMemberId(ChatMessageRequest request, Pageable pageable) {

        List<ChatMessage> messages = chatMessageRepository.findByChatIdAndChatMemberId(request.chatId(), request.memberId(), pageable);
        return messages.stream()
                .map(chatMessageMapper::toDto)
                .toList();
    }

    @Transactional
    public void markAsDelivered(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElseThrow(
                () -> new ChatMessageNotFoundException("Chat message not found with id: " + messageId));
        message.setStatus(MessageStatus.DELIVERED);
    }

    @Override
    public List<Long> markAsRead(Long chatId, Long userId, String status) {
        return chatMessageRepository.markMessagesAsReadAndReturnIds(chatId, userId, status);
    }

    @Override
    public ChatMessageDto getMessageById(long messageId) {
        return chatMessageMapper.toDto(chatMessageRepository.findById(messageId).orElseThrow(()
                -> new ChatMessageNotFoundException(
                "Chat message not found with id: %d".formatted(messageId)
        )));
    }

}
