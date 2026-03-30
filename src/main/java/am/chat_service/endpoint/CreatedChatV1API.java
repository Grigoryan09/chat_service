package am.chat_service.endpoint;

import am.chat_service.dto.request.CreateChatFromExternalRequest;
import am.chat_service.dto.response.CreatedChatIdResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat-service")
public interface CreatedChatV1API {

    @PostMapping("/create-chat-from-external")
    ResponseEntity<CreatedChatIdResponse> createChatFromExternal(
            @RequestBody @Valid CreateChatFromExternalRequest request);
}
