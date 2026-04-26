package am.chat_service.dto.response;

public record ServiceConfigResponse(
        String applicationName,
        int serverPort,
        String socketHost,
        int socketPort,
        String swaggerUiPath,
        String apiDocsPath
) {
}
