package am.chat_service.endpoint;

import am.chat_service.dto.ChatMessageDto;
import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.request.EditMessageRequest;
import am.chat_service.dto.response.ChatResponse;
import am.chat_service.dto.response.UserChatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-service/api/v1/chats")
@Tag(name = "Chats", description = "Endpoints for creating chats and retrieving chat details")
public interface CreatedChatV1API {

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create chat", description = "Creates a new chat from an external request payload.")
    ChatResponse createChatFromExternal(
            @RequestBody @Valid CreateChatRequest request);


    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get chat details", description = "Returns chat metadata together with paginated messages.")
    ChatResponse getChatDetail(
            @PathVariable Long id,
            Pageable pageable
    );

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List user chats", description = "Returns a paginated list of chats the user participates in, most recently active first.")
    UserChatsResponse getUserChats(
            @RequestParam Long userId,
            Pageable pageable
    );

    @PutMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Edit message", description = "Updates the text of an existing message and broadcasts the change to the chat room.")
    ChatMessageDto updateMessage(
            @PathVariable Long messageId,
            @RequestBody @Valid EditMessageRequest request
    );

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete message", description = "Deletes a message and broadcasts the removal to the chat room.")
    void deleteMessage(@PathVariable Long messageId);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete chat", description = "Soft-deletes a chat by archiving it and notifies participants.")
    void deleteChat(@PathVariable Long id);
}
