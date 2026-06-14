package transpadang.spm.transpadang_final.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket native (Spring) untuk kanal {@code /ws/notifications}.
 *
 * <p>Menyimpan seluruh sesi aktif sehingga server dapat melakukan broadcast,
 * mis. ketika status penilaian berubah. Pesan masuk akan di-echo balik ke
 * pengirim sebagai default.
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {} (total {})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        log.debug("WebSocket message from {}: {}", session.getId(), message.getPayload());
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(message.getPayload()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {} ({}) (total {})", session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Kirim pesan teks ke seluruh sesi yang masih terbuka.
     */
    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.warn("Failed to send to {}: {}", session.getId(), e.getMessage());
            }
        });
    }
}
