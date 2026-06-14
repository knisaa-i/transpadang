package transpadang.spm.transpadang_final.config;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SocketIOConfig {

    @Value("${app.socketio.host}")
    private String host;

    @Value("${app.socketio.port}")
    private Integer port;

    @Value("${app.socketio.boss-count:1}")
    private int bossCount;

    @Value("${app.socketio.worker-count:100}")
    private int workerCount;

    @Value("${app.socketio.ping-timeout:60000}")
    private int pingTimeout;

    @Value("${app.socketio.ping-interval:25000}")
    private int pingInterval;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(bossCount);
        config.setWorkerThreads(workerCount);
        config.setJsonSupport(new SocketIoJsonSupport());

        // Socket stability configurations
        config.setPingInterval(pingInterval);
        config.setPingTimeout(pingTimeout);
        config.setOrigin(null); // Set to null to dynamically echo back the incoming Origin header (required when credentials/cookies are used)

        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        socketConfig.setTcpKeepAlive(true);
        config.setSocketConfig(socketConfig);

        log.info("Configuring SocketIOServer on {}:{}", host, port);
        // NOTE: server di-start oleh SocketIOServerRunner (bukan di sini), agar
        // lifecycle start/stop mengikuti lifecycle Spring context.
        return new SocketIOServer(config);
    }
}
