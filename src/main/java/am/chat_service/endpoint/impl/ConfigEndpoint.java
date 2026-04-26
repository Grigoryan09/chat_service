package am.chat_service.endpoint.impl;

import am.chat_service.dto.response.ServiceConfigResponse;
import am.chat_service.endpoint.ConfigV1API;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigEndpoint implements ConfigV1API {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${socketio.hostname}")
    private String socketHost;

    @Value("${socketio.port}")
    private int socketPort;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Override
    public ServiceConfigResponse getConfig() {
        return new ServiceConfigResponse(
                applicationName,
                serverPort,
                socketHost,
                socketPort,
                swaggerUiPath,
                apiDocsPath
        );
    }
}
