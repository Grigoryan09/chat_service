package am.chat_service.service.impl;

import am.chat_service.dto.ChatMemberDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.exception.ChatMemberNotFoundException;
import am.chat_service.mapper.ChatMemberMapper;
import am.chat_service.model.Chat;
import am.chat_service.model.ChatMember;
import am.chat_service.model.enums.ChatType;
import am.chat_service.repository.ChatMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatMemberServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ChatMemberServiceImplTest {

    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private ChatMemberMapper chatMemberMapper;

    @InjectMocks
    private ChatMemberServiceImpl service;

    // ---- findById ----------------------------------------------------------

    @Test
    void findById_returnsMappedDto() {
        ChatMember member = new ChatMember();
        member.setId(2L);
        ChatMemberDto dto = new ChatMemberDto(2L, 5L);
        when(chatMemberRepository.findOptionalById(2L)).thenReturn(Optional.of(member));
        when(chatMemberMapper.toDto(member)).thenReturn(dto);

        assertThat(service.findById(2L)).isSameAs(dto);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(chatMemberRepository.findOptionalById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(2L))
                .isInstanceOf(ChatMemberNotFoundException.class)
                .hasMessage("Chat member not found with id: 2");
    }

    // ---- getChatMembers ----------------------------------------------------

    @Test
    void getChatMembers_buildsOneMemberPerUserIdLinkedToChat() {
        Chat chat = new Chat();
        chat.setId(1L);
        CreateChatRequest request =
                new CreateChatRequest(List.of(10L, 20L, 30L), ChatType.GROUP);

        List<ChatMember> members = service.getChatMembers(request, chat);

        assertThat(members).hasSize(3);
        assertThat(members).allMatch(m -> m.getChat() == chat);
        assertThat(members).extracting(ChatMember::getUserId)
                .containsExactly(10L, 20L, 30L);
    }

    // ---- getMembersByChatId ------------------------------------------------

    @Test
    void getMembersByChatId_returnsUserIds() {
        when(chatMemberRepository.findUserIdsByChatId(1L))
                .thenReturn(Optional.of(List.of(10L, 20L)));

        assertThat(service.getMembersByChatId(1L)).containsExactly(10L, 20L);
    }

    @Test
    void getMembersByChatId_returnsEmptyListWhenNoneFound() {
        when(chatMemberRepository.findUserIdsByChatId(1L)).thenReturn(Optional.empty());

        assertThat(service.getMembersByChatId(1L)).isEmpty();
    }
}
