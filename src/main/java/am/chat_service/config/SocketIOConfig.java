package am.chat_service.config;


import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.hostname}")
    private String hostname;

    @Value("${socketio.port}")
    private int port;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {

        com.corundumstudio.socketio.Configuration config =
                new com.corundumstudio.socketio.Configuration();

        config.setHostname(hostname);
        config.setPort(port);
        config.setOrigin("*");
        config.setAllowCustomRequests(true);
        config.setPingInterval(25000);
        config.setPingTimeout(60000);


        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()));

        return new SocketIOServer(config);
    }





}