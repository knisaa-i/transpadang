package transpadang.spm.transpadang_final.service;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Menjalankan dan menghentikan {@link SocketIOServer} mengikuti lifecycle Spring,
 * serta mendaftarkan listener dasar (connect/disconnect/message).
 *
 * <p>Gunakan {@link #broadcast(String, Object)} dari service lain untuk mengirim
 * event ke seluruh client Socket.IO yang terhubung.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocketIOServerRunner {

    private final SocketIOServer server;

    @PostConstruct
    public void start() {
        server.addConnectListener(client ->
                log.info("Socket.IO client connected: {}", client.getSessionId()));

        server.addDisconnectListener(client ->
                log.info("Socket.IO client disconnected: {}", client.getSessionId()));

        // Event "message": echo + broadcast ke semua client
        server.addEventListener("message", String.class, (client, data, ackRequest) -> {
            log.debug("Socket.IO message from {}: {}", client.getSessionId(), data);
            server.getBroadcastOperations().sendEvent("message", data);
        });

        server.start();
        log.info("Socket.IO server started");
    }

    @PreDestroy
    public void stop() {
        server.stop();
        log.info("Socket.IO server stopped");
    }

    /**
     * Broadcast event Socket.IO ke seluruh client yang terhubung.
     */
    public void broadcast(String event, Object data) {
        server.getBroadcastOperations().sendEvent(event, data);
    }
}
