package am.chat_service.service.impl;

import am.chat_service.dto.mapper.ChatMessageMapper;
import am.chat_service.dto.request.ChatMessageRequest;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.dto.ChatMessageDto;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatMessageNotFoundException;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.InvalidChatRequestException;
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

        Chat chat = chatRepository.findById(request.getChatId()).orElseThrow(() -> new ChatNotFoundException("Chat not found with id: " + request.getChatId()));
        ChatMember member = chatMemberRepository.findById(request.getMemberId());
        if (!Objects.equals(member.getChat().getId(), chat.getId())) {
            throw new InvalidChatRequestException(
                    "Member " + member.getId() + " does not belong to chat " + chat.getId()
            );
        }

        chat.setLastActivity(LocalDateTime.now());
        chatRepository.save(chat);

        ChatMessageDto messageDto = chatMessageMapper.toDto(
                chatMessageRepository.save(createMessage(request, chat, member)));

        chatEventPublisher.publishNewMessage(request.getChatId(), messageDto);

        return messageDto;
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
    public void updateChatMessage(UpdateMessageRequest request) {

        ChatMessage message = chatMessageRepository.findById(request.messageId())
                .orElseThrow(()
                        -> new ChatMessageNotFoundException("Chat message not found with id:" + request.messageId()));
        message.setMessage(request.message());
        message.setUpdatedDate(LocalDateTime.now());
        chatMessageRepository.save(message);
    }

    @Override
    public void deleteChatMessage(long messageId) {
        chatMessageRepository.deleteById(messageId);
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
    public List<Long> markAsRead(Long chatId, Long userId, MessageStatus status) {
        return  chatMessageRepository.markMessagesAsReadAndReturnIds(chatId, userId, MessageStatus.READ);
    }

    @Override
    public ChatMessageDto getMessageById(long messageId) {
        return chatMessageMapper.toDto(chatMessageRepository.findById(messageId).orElseThrow(()
                -> new ChatMessageNotFoundException("Chat message not found with id: " + messageId)));
    }

}
