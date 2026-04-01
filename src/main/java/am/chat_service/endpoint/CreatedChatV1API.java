package am.chat_service.endpoint;

import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for managing chat resources.
 *
 * <p>This controller provides endpoints for creating and managing chats
 * within the chat service.</p>
 *
 * <p>Base path: <b>/api/v1/chat-service</b></p>
 */
@RestController
@RequestMapping("/api/v1/chat-service")
public interface CreatedChatV1API {

    /**
     * Creates a new chat.
     *
     * <p>This endpoint is typically used by external systems or services
     * to initialize a new chat with predefined participants or metadata.</p>
     *
     * <p>The request must contain all required information to create a chat,
     * such as participant identifiers and any additional attributes.</p>
     *
     * @param request the request payload containing chat creation details
     * @return {@link ChatResponse} containing information about the created chat
     *
     * @throws jakarta.validation.ConstraintViolationException if validation fails
     */
    @PostMapping("/chats")
    ChatResponse createChatFromExternal(
            @RequestBody @Valid CreateChatRequest request);
}
