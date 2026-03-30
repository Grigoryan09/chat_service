package am.chat_service.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServerCommandLineRunner implements CommandLineRunner {

    private final SocketIOServer server;

    @Override
    public void run(String... args) {
        server.start();
        log.info("Socket.IO server started on {}:{}",
                server.getConfiguration().getHostname(),
                server.getConfiguration().getPort());
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.stop();
            log.info("Socket.IO server stopped.");
        }
    }
}