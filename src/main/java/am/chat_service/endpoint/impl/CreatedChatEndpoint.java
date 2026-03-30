package am.chat_service.endpoint.impl;

import am.chat_service.dto.request.CreateChatFromExternalRequest;
import am.chat_service.dto.response.CreatedChatIdResponse;
import am.chat_service.endpoint.CreatedChatV1API;
import am.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Component
public class CreatedChatEndpoint implements CreatedChatV1API {

    private final ChatService chatService;

    @Override
    public ResponseEntity<CreatedChatIdResponse> createChatFromExternal(CreateChatFromExternalRequest request) {
        return ResponseEntity.ok(new CreatedChatIdResponse(chatService.createChatFromExternal(request)));
    }
}
