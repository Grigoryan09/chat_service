package am.chat_service.service.impl;

import am.chat_service.dto.mapper.ChatMessageMapper;
import am.chat_service.dto.request.SendMessageRequest;
import am.chat_service.dto.request.UpdateMessageRequest;
import am.chat_service.dto.response.ChatMessageDto;
import am.chat_service.event.ChatEventPublisher;
import am.chat_service.exception.ChatMemberNotFoundException;
import am.chat_service.exception.ChatMessageNotFoundException;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.exception.InvalidChatRequestException;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.ChatMessage;
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
    public ChatMessageDto sendMessage(SendMessageRequest sendMessageRequest) {
        if (sendMessageRequest == null) {
            throw new InvalidChatRequestException("SendMessageRequest cannot be null");
        }

        if (sendMessageRequest.getChatId() <= 0) {
            throw new InvalidChatRequestException("Chat ID must be positive" + sendMessageRequest.getChatId() );
        }

        if (sendMessageRequest.getMemberId() <= 0) {
            throw new InvalidChatRequestException("Member ID must be positive" + sendMessageRequest.getMemberId());
        }

        if (sendMessageRequest.getMessage() == null || sendMessageRequest.getMessage().isEmpty()) {
            throw new InvalidChatRequestException("Message cannot be empty" + sendMessageRequest.getMessage());
        }

        Chat chat = chatRepository.findById(sendMessageRequest.getChatId()).orElseThrow(() -> new ChatNotFoundException(sendMessageRequest.getChatId()));
        ChatMember member = chatMemberRepository.findById(sendMessageRequest.getMemberId()).orElseThrow(
                () -> new ChatMemberNotFoundException(sendMessageRequest.getMemberId()));
        if (!Objects.equals(member.getChat().getId(), chat.getId())) {
            throw new InvalidChatRequestException(
                    "Member " + member.getId() + " does not belong to chat " + chat.getId()
            );
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setMessage(sendMessageRequest.getMessage());
        message.setChatMember(member);
        message.setCreatedDate(LocalDateTime.now());
        message.setUpdatedDate(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        chat.setLastActivity(LocalDateTime.now());
        chatRepository.save(chat);

        ChatMessageDto messageDto = chatMessageMapper.toDto(savedMessage);

        chatEventPublisher.publishNewMessage(sendMessageRequest.getChatId(), messageDto);

        return messageDto;
    }

    @Override
    public void updateChatMessage(UpdateMessageRequest updateMessageReadRequest) {
        if (updateMessageReadRequest == null) {
            throw new InvalidChatRequestException("UpdateMessageRequest cannot be null");
        }

        if (updateMessageReadRequest.messageId() <= 0) {
            throw new InvalidChatRequestException("messageId must be positive" + updateMessageReadRequest.messageId());
        }

        ChatMessage message = chatMessageRepository.findById(updateMessageReadRequest.messageId())
                .orElseThrow(()
                        -> new ChatMessageNotFoundException("Chat message not found with id:" + updateMessageReadRequest.messageId()));

        if (updateMessageReadRequest.message() != null && !updateMessageReadRequest.message().isEmpty()) {
            message.setMessage(updateMessageReadRequest.message());
        }

        message.setUpdatedDate(LocalDateTime.now());
        chatMessageRepository.save(message);
    }

    @Override
    public void deleteChatMessage(long chatMessageId) {
        if (chatMessageId <= 0) {
            throw new InvalidChatRequestException("Chat Message ID must be positive" + chatMessageId);
        }

        if (!chatMessageRepository.existsById(chatMessageId)) {
            throw new ChatMessageNotFoundException("Chat message not found with id: " + chatMessageId);
        }

        chatMessageRepository.deleteById(chatMessageId);
    }

    @Override
    public List<ChatMessageDto> getChatMessagesByMemberId(long chatId, long memberId, Pageable pageable) {
        if (chatId <= 0) {
            throw new InvalidChatRequestException("Chat ID must be positive" + chatId);
        }
        if (memberId <= 0) {
            throw new InvalidChatRequestException("Member ID must be positive" + memberId);
        }

        if (!chatRepository.existsById(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatIdAndChatMemberId(chatId, memberId, pageable);
        return messages.stream()
                .map(chatMessageMapper::toDto)
                .toList();
    }

}
