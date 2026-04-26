package am.chat_service.endpoint;

import am.chat_service.dto.response.ServiceConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-service/api/v1/config")
@Tag(name = "Service Config", description = "Endpoints for chat service runtime configuration")
public interface ConfigV1API {

    @GetMapping
    @Operation(summary = "Get service config", description = "Returns HTTP, Socket.IO and OpenAPI endpoint settings.")
    ServiceConfigResponse getConfig();
}
