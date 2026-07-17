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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatMessageServiceImpl}. Collaborators are mocked; the
 * tests assert the persistence side-effects and the events published after a
 * successful operation (or that no event fires on the failure paths).
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatEventPublisher chatEventPublisher;

    @InjectMocks
    private ChatMessageServiceImpl service;

    private static ChatMessageDto dto() {
        return new ChatMessageDto(1L, 1L, "hi", MessageStatus.SENT, null, null);
    }

    private static SendMessageRequest sendRequest(long chatId, long memberId, String message) {
        SendMessageRequest request = new SendMessageRequest();
        request.setChatId(chatId);
        request.setMemberId(memberId);
        request.setMessage(message);
        return request;
    }

    // ---- sendMessage -------------------------------------------------------

    @Test
    void sendMessage_persistsMessageUpdatesActivityAndPublishes() {
        Chat chat = new Chat();
        chat.setId(1L);
        ChatMember member = new ChatMember();
        member.setId(2L);
        member.setChat(chat);
        ChatMessageDto dto = dto();

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findOptionalById(2L)).thenReturn(Optional.of(member));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(dto);

        ChatMessageDto result = service.sendMessage(sendRequest(1L, 2L, "hello"));

        assertThat(result).isSameAs(dto);
        assertThat(chat.getLastActivity()).isNotNull();
        verify(chatRepository).save(chat);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(saved.getMessage()).isEqualTo("hello");
        assertThat(saved.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(saved.getChat()).isSameAs(chat);
        assertThat(saved.getChatMember()).isSameAs(member);

        verify(chatEventPublisher).publishNewMessage(1L, dto);
    }

    @Test
    void sendMessage_throwsWhenChatMissing() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(sendRequest(1L, 2L, "hi")))
                .isInstanceOf(ChatNotFoundException.class)
                .hasMessage("Chat not found with id: 1");

        verifyNoInteractions(chatEventPublisher);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_throwsWhenMemberMissing() {
        Chat chat = new Chat();
        chat.setId(1L);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findOptionalById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(sendRequest(1L, 2L, "hi")))
                .isInstanceOf(ChatMemberNotFoundException.class)
                .hasMessage("Chat member not found with id: 2");

        verifyNoInteractions(chatEventPublisher);
    }

    @Test
    void sendMessage_throwsWhenMemberBelongsToAnotherChat() {
        Chat chat = new Chat();
        chat.setId(1L);
        Chat otherChat = new Chat();
        otherChat.setId(99L);
        ChatMember member = new ChatMember();
        member.setId(2L);
        member.setChat(otherChat);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findOptionalById(2L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.sendMessage(sendRequest(1L, 2L, "hi")))
                .isInstanceOf(InvalidChatRequestException.class)
                .hasMessage("Member 2 does not belong to chat 1");

        verify(chatMessageRepository, never()).save(any());
        verifyNoInteractions(chatEventPublisher);
    }

    // ---- sendDocumentMessage ----------------------------------------------

    @Test
    void sendDocumentMessage_persistsAndPublishes() {
        Chat chat = new Chat();
        chat.setId(1L);
        ChatMember member = new ChatMember();
        member.setId(2L);
        ChatMessageDto dto = dto();

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 5L)).thenReturn(Optional.of(member));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(dto);

        ChatMessageDto result = service.sendDocumentMessage(1L, 5L, "document.pdf");

        assertThat(result).isSameAs(dto);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("document.pdf");
        verify(chatEventPublisher).publishNewMessage(1L, dto);
    }

    @Test
    void sendDocumentMessage_throwsWhenMemberNotInChat() {
        Chat chat = new Chat();
        chat.setId(1L);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendDocumentMessage(1L, 5L, "x"))
                .isInstanceOf(ChatMemberNotFoundException.class)
                .hasMessage("Chat member not found in chat 1 for user 5");

        verifyNoInteractions(chatEventPublisher);
    }

    // ---- updateChatMessage -------------------------------------------------

    @Test
    void updateChatMessage_updatesTextAndPublishes() {
        Chat chat = new Chat();
        chat.setId(7L);
        ChatMessage message = new ChatMessage();
        message.setId(3L);
        message.setChat(chat);
        message.setMessage("old");
        ChatMessageDto dto = dto();

        when(chatMessageRepository.findById(3L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.save(message)).thenReturn(message);
        when(chatMessageMapper.toDto(message)).thenReturn(dto);

        ChatMessageDto result = service.updateChatMessage(new UpdateMessageRequest(3L, "new text"));

        assertThat(result).isSameAs(dto);
        assertThat(message.getMessage()).isEqualTo("new text");
        assertThat(message.getUpdatedDate()).isNotNull();
        verify(chatEventPublisher).publishMessageUpdated(7L, dto);
    }

    @Test
    void updateChatMessage_throwsWhenMessageMissing() {
        when(chatMessageRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateChatMessage(new UpdateMessageRequest(3L, "x")))
                .isInstanceOf(ChatMessageNotFoundException.class)
                .hasMessage("Chat message not found with id:3");

        verifyNoInteractions(chatEventPublisher);
    }

    // ---- deleteChatMessage -------------------------------------------------

    @Test
    void deleteChatMessage_deletesAndPublishes() {
        Chat chat = new Chat();
        chat.setId(7L);
        ChatMessage message = new ChatMessage();
        message.setId(3L);
        message.setChat(chat);

        when(chatMessageRepository.findById(3L)).thenReturn(Optional.of(message));

        service.deleteChatMessage(3L);

        verify(chatMessageRepository).delete(message);
        verify(chatEventPublisher).publishMessageDeleted(7L, 3L);
    }

    @Test
    void deleteChatMessage_throwsWhenMissing() {
        when(chatMessageRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteChatMessage(3L))
                .isInstanceOf(ChatMessageNotFoundException.class)
                .hasMessage("Chat message not found with id: 3");

        verify(chatMessageRepository, never()).delete(any());
        verifyNoInteractions(chatEventPublisher);
    }

    // ---- getChatMessagesByMemberId ----------------------------------------

    @Test
    void getChatMessagesByMemberId_mapsEachMessage() {
        ChatMessage m1 = new ChatMessage();
        ChatMessage m2 = new ChatMessage();
        Pageable pageable = Pageable.ofSize(10);
        ChatMessageDto d1 = dto();
        ChatMessageDto d2 = dto();

        when(chatMessageRepository.findByChatIdAndChatMemberId(1L, 2L, pageable))
                .thenReturn(List.of(m1, m2));
        when(chatMessageMapper.toDto(m1)).thenReturn(d1);
        when(chatMessageMapper.toDto(m2)).thenReturn(d2);

        List<ChatMessageDto> result =
                service.getChatMessagesByMemberId(new ChatMessageRequest(1L, 2L), pageable);

        assertThat(result).containsExactly(d1, d2);
    }

    // ---- markAsDelivered ---------------------------------------------------

    @Test
    void markAsDelivered_setsStatusDelivered() {
        ChatMessage message = new ChatMessage();
        message.setId(3L);
        message.setStatus(MessageStatus.SENT);
        when(chatMessageRepository.findById(3L)).thenReturn(Optional.of(message));

        service.markAsDelivered(3L);

        assertThat(message.getStatus()).isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    void markAsDelivered_throwsWhenMissing() {
        when(chatMessageRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsDelivered(3L))
                .isInstanceOf(ChatMessageNotFoundException.class);
    }

    // ---- markAsRead --------------------------------------------------------

    @Test
    void markAsRead_delegatesToRepository() {
        when(chatMessageRepository.markMessagesAsReadAndReturnIds(1L, 5L, "READ"))
                .thenReturn(List.of(10L, 11L));

        List<Long> ids = service.markAsRead(1L, 5L, "READ");

        assertThat(ids).containsExactly(10L, 11L);
    }

    // ---- getMessageById ----------------------------------------------------

    @Test
    void getMessageById_returnsMappedDto() {
        ChatMessage message = new ChatMessage();
        ChatMessageDto dto = dto();
        when(chatMessageRepository.findById(3L)).thenReturn(Optional.of(message));
        when(chatMessageMapper.toDto(message)).thenReturn(dto);

        assertThat(service.getMessageById(3L)).isSameAs(dto);
    }

    @Test
    void getMessageById_throwsWhenMissing() {
        when(chatMessageRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessageById(3L))
                .isInstanceOf(ChatMessageNotFoundException.class)
                .hasMessage("Chat message not found with id: 3");
    }
}
