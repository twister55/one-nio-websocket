import java.io.IOException;
import java.util.Collections;

import one.nio.server.AcceptorConfig;
import one.nio.ws.WebSocketServer;
import one.nio.ws.WebSocketServerConfig;
import one.nio.ws.WebSocketSession;
import one.nio.ws.message.TextMessage;

public class EchoServer extends WebSocketServer {

    public EchoServer(WebSocketServerConfig config) throws IOException {
        super(config);
    }

    @Override
    public void handleMessage(WebSocketSession session, TextMessage message) throws IOException {
        session.sendMessage(new TextMessage(message.payload()));
    }

    public static void main(String[] args) throws IOException {
        EchoServer server = new EchoServer(config());
        server.registerShutdownHook();
        server.start();
    }

    private static WebSocketServerConfig config() {
        WebSocketServerConfig config = new WebSocketServerConfig();
        config.supportedProtocols = Collections.singleton("echo1");
        config.websocketBaseUri = "/echo";
        config.keepAlive = 30000;
        config.maxWorkers = 1000;
        config.queueTime = 50;
        config.acceptors = acceptors();
        return config;
    }

    private static AcceptorConfig[] acceptors() {
        AcceptorConfig config = new AcceptorConfig();
        config.port = 8002;
        config.backlog = 10000;
        config.deferAccept = true;
        return new AcceptorConfig[] {
                config
        };
    }
}
