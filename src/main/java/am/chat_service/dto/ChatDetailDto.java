package am.chat_service.dto;

import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class ChatDetailDto {

    private String status;
    private String message;
    private LocalDateTime timestamp;

    private Long id;
    private ChatType chatType;
    private ChatStatus chatStatus;
    private LocalDateTime lastActivity;
    private List<ChatMemberDto> members;
    private Page<ChatMessageDto> messages;
}
