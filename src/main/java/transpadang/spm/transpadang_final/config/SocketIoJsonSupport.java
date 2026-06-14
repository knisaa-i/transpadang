package transpadang.spm.transpadang_final.config;

import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Dukungan serialisasi JSON untuk netty-socketio berbasis Jackson.
 *
 * <p>Mendaftarkan {@link JavaTimeModule} agar tipe {@code java.time}
 * (mis. {@code LocalDateTime}) bisa di-serialize pada event Socket.IO.
 */
public class SocketIoJsonSupport extends JacksonJsonSupport {

    public SocketIoJsonSupport() {
        super(new JavaTimeModule());
    }
}
