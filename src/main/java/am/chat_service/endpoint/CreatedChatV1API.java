package am.chat_service.endpoint;

import am.chat_service.dto.request.CreateChatRequest;
import am.chat_service.dto.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/chat-service/api/v1/chats")
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
     * @throws jakarta.validation.ConstraintViolationException if validation fails
     */
    @PostMapping()
    ChatResponse createChatFromExternal(
            @RequestBody @Valid CreateChatRequest request);


    /**
     * Retrieves detailed information about a chat along with its paginated messages.
     *
     * <p>This endpoint is used to load a chat in the frontend. It returns basic chat
     * information (name, type, status, members, etc.) and a page of messages with
     * support for pagination and sorting.</p>
     *
     * @param id       the ID of the chat
     * @param pageable pagination and sorting information.
     *                 Supported parameters:
     *                 <ul>
     *                   <li>{@code page} - page number (zero-based)</li>
     *                   <li>{@code size} - number of messages per page</li>
     *                   <li>{@code sort} - sorting criteria (e.g. {@code createdAt,desc})</li>
     *                 </ul>
     * @return {@link ChatResponse} containing chat details and a page of messages
     *
     */
    @GetMapping("/{id}")
    ChatResponse getChatDetail(
            @PathVariable Long id,
            Pageable pageable
    );
}
