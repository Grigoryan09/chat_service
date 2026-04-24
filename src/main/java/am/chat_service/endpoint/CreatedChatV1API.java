package am.chat_service.endpoint;

import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.response.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
