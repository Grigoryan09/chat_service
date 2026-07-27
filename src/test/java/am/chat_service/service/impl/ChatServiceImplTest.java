package am.chat_service.service.impl;

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
import am.chat_service.repository.ChatMessageRepository;
import am.chat_service.repository.ChatRepository;
import am.chat_service.service.ChatMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatServiceImpl}. Collaborators are mocked so the tests
 * exercise the service's own branching, validation and event-publishing logic.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatEventPublisher chatEventPublisher;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private ChatMemberMapper chatMemberMapper;
    @Mock
    private ChatMemberService chatMemberService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static ChatMessageDto messageDto() {
        return new ChatMessageDto(1L, 1L, "Chat created", null, null, null);
    }

    // ---- validateUsers -----------------------------------------------------

    @Test
    void createChatFromExternal_reportsOnlyOffendingDuplicateId() {
        // buyerId=15, sellerId=14, managerId=15 -> the production incident payload
        CreateChatRequest request =
                new CreateChatRequest(List.of(15L, 14L, 15L), ChatType.GROUP, null);

        assertThatThrownBy(() -> chatService.createChatFromExternal(request))
                .isInstanceOf(DuplicateUserInChatException.class)
                .hasMessage("Duplicate user IDs found in the request: [15]");
    }

    @Test
    void createChatFromExternal_listsEachDuplicateOnceInEncounterOrder() {
        CreateChatRequest request =
                new CreateChatRequest(List.of(15L, 14L, 15L, 14L, 15L), ChatType.GROUP, null);

        assertThatThrownBy(() -> chatService.createChatFromExternal(request))
                .isInstanceOf(DuplicateUserInChatException.class)
                .hasMessage("Duplicate user IDs found in the request: [15, 14]");
    }

    @Test
    void createChatFromExternal_withDuplicates_touchesNoCollaborators() {
        CreateChatRequest request =
                new CreateChatRequest(List.of(1L, 1L), ChatType.GROUP, null);

        assertThatThrownBy(() -> chatService.createChatFromExternal(request))
                .isInstanceOf(DuplicateUserInChatException.class);

        verify(chatRepository, never()).save(any());
        verify(chatEventPublisher, never()).publishChatOpened(any(), anyList(), any());
    }

    // ---- createChatFromExternal (happy path) -------------------------------

    @Test
    void createChatFromExternal_persistsChatFirstMessageAndPublishesEvents() {
        List<Long> userIds = List.of(101L, 202L);
        CreateChatRequest request = new CreateChatRequest(userIds, ChatType.ONE_TO_ONE, null);

        Chat savedChat = new Chat();
        savedChat.setId(42L);
        savedChat.setChatType(ChatType.ONE_TO_ONE);
        savedChat.setChatMessages(new ArrayList<>());

        ChatMessageDto firstMsgDto = messageDto();

        when(chatMemberService.getChatMembers(eq(request), any(Chat.class))).thenReturn(List.of());
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(firstMsgDto);
        when(chatMapper.toDto(savedChat)).thenReturn(ChatDetailDto.builder().id(42L).build());

        ChatDetailDto result = chatService.createChatFromExternal(request);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("Chat created successfully");
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);

        verify(chatEventPublisher).publishChatOpened(eq(42L), eq(userIds), any());
        verify(chatEventPublisher).publishNewMessage(42L, firstMsgDto);
    }

    // ---- createChatFromExternal (idempotency by orderId) -------------------

    @Test
    void createChatFromExternal_orderAlreadyHasChat_returnsExistingWithoutCreatingDuplicate() {
        CreateChatRequest request = new CreateChatRequest(List.of(101L, 202L, 303L), ChatType.GROUP, 7L);

        Chat existing = new Chat();
        existing.setId(42L);
        existing.setOrderId(7L);

        when(chatRepository.findByOrderId(7L)).thenReturn(Optional.of(existing));
        when(chatMapper.toDto(existing)).thenReturn(ChatDetailDto.builder().id(42L).build());

        ChatDetailDto result = chatService.createChatFromExternal(request);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("Chat already exists for this order");

        verify(chatRepository, never()).save(any());
        verify(chatMessageRepository, never()).save(any());
        verify(chatEventPublisher, never()).publishChatOpened(any(), anyList(), any());
    }

    @Test
    void createChatFromExternal_orderIdSetAndUnused_persistsChatCarryingOrderId() {
        List<Long> userIds = List.of(101L, 202L, 303L);
        CreateChatRequest request = new CreateChatRequest(userIds, ChatType.GROUP, 7L);

        Chat savedChat = new Chat();
        savedChat.setId(42L);
        savedChat.setOrderId(7L);
        savedChat.setChatType(ChatType.GROUP);
        savedChat.setChatMessages(new ArrayList<>());

        when(chatRepository.findByOrderId(7L)).thenReturn(Optional.empty());
        when(chatMemberService.getChatMembers(eq(request), any(Chat.class))).thenReturn(List.of());
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(messageDto());
        when(chatMapper.toDto(savedChat)).thenReturn(ChatDetailDto.builder().id(42L).build());

        ChatDetailDto result = chatService.createChatFromExternal(request);

        assertThat(result.getId()).isEqualTo(42L);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(7L);
    }

    @Test
    void createChatFromExternal_defaultsToOneToOneForTwoUsersWhenTypeNull() {
        CreateChatRequest request = new CreateChatRequest(List.of(1L, 2L), null, null);

        Chat savedChat = new Chat();
        savedChat.setId(7L);
        savedChat.setChatMessages(new ArrayList<>());

        when(chatMemberService.getChatMembers(any(), any())).thenReturn(List.of());
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(messageDto());
        when(chatMapper.toDto(savedChat)).thenReturn(ChatDetailDto.builder().build());

        chatService.createChatFromExternal(request);

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(chatCaptor.capture());
        assertThat(chatCaptor.getValue().getChatType()).isEqualTo(ChatType.ONE_TO_ONE);
        assertThat(chatCaptor.getValue().getChatStatus()).isEqualTo(ChatStatus.ACTIVE);
    }

    @Test
    void createChatFromExternal_defaultsToGroupForMoreThanTwoUsersWhenTypeNull() {
        CreateChatRequest request = new CreateChatRequest(List.of(1L, 2L, 3L), null, null);

        Chat savedChat = new Chat();
        savedChat.setId(8L);
        savedChat.setChatMessages(new ArrayList<>());

        when(chatMemberService.getChatMembers(any(), any())).thenReturn(List.of());
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(messageDto());
        when(chatMapper.toDto(savedChat)).thenReturn(ChatDetailDto.builder().build());

        chatService.createChatFromExternal(request);

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(chatCaptor.capture());
        assertThat(chatCaptor.getValue().getChatType()).isEqualTo(ChatType.GROUP);
    }

    // ---- changeChatStatus --------------------------------------------------

    @Test
    void changeChatStatus_updatesStatusAndSaves() {
        Chat chat = new Chat();
        chat.setId(5L);
        chat.setChatStatus(ChatStatus.ACTIVE);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(chat));

        chatService.changeChatStatus(new ChangeChatStatusRequest(5L, ChatStatus.ARCHIVED));

        assertThat(chat.getChatStatus()).isEqualTo(ChatStatus.ARCHIVED);
        verify(chatRepository).save(chat);
    }

    @Test
    void changeChatStatus_throwsWhenChatMissing() {
        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.changeChatStatus(
                new ChangeChatStatusRequest(99L, ChatStatus.ARCHIVED)))
                .isInstanceOf(ChatNotFoundException.class)
                .hasMessage("Chat with id 99 not found");

        verify(chatRepository, never()).save(any());
    }

    // ---- findById ----------------------------------------------------------

    @Test
    void findById_returnsMappedDtoWhenFound() {
        Chat chat = new Chat();
        chat.setId(3L);
        ChatDetailDto dto = ChatDetailDto.builder().id(3L).build();
        when(chatRepository.findById(3L)).thenReturn(Optional.of(chat));
        when(chatMapper.toDto(chat)).thenReturn(dto);

        assertThat(chatService.findById(3L)).isSameAs(dto);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(chatRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.findById(3L))
                .isInstanceOf(ChatNotFoundException.class)
                .hasMessage("Chat with id 3 not found");
    }

    // ---- deleteChat (soft delete) ------------------------------------------

    @Test
    void deleteChat_archivesChatAndPublishesEvent() {
        Chat chat = new Chat();
        chat.setId(11L);
        chat.setChatStatus(ChatStatus.ACTIVE);
        when(chatRepository.findById(11L)).thenReturn(Optional.of(chat));

        chatService.deleteChat(11L);

        assertThat(chat.getChatStatus()).isEqualTo(ChatStatus.ARCHIVED);
        verify(chatRepository).save(chat);
        verify(chatEventPublisher).publishChatArchived(11L);
    }

    @Test
    void deleteChat_throwsWhenMissingAndPublishesNothing() {
        when(chatRepository.findById(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteChat(11L))
                .isInstanceOf(ChatNotFoundException.class);

        verify(chatEventPublisher, never()).publishChatArchived(any());
    }

    // ---- getUserChats ------------------------------------------------------

    @Test
    void getUserChats_mapsPageToSummaries() {
        Chat chat = new Chat();
        chat.setId(1L);
        Page<Chat> page = new PageImpl<>(List.of(chat));
        ChatSummaryDto summary = new ChatSummaryDto(1L, null, null, null, null);
        Pageable pageable = Pageable.ofSize(10);

        when(chatRepository.findChatsByUserId(77L, pageable)).thenReturn(page);
        when(chatMapper.toSummary(chat)).thenReturn(summary);

        UserChatsDto result = chatService.getUserChats(77L, pageable);

        assertThat(result.chats().getContent()).containsExactly(summary);
    }
}
