import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import one.nio.ws.WebSocketServer;
import one.nio.ws.WebSocketSession;
import one.nio.ws.message.TextMessage;
import one.nio.ws.message.Message;

public class EchoServer extends WebSocketServer {
    private static final Log log = LogFactory.getLog(EchoServer.class);

    public EchoServer(HttpServerConfig config) throws IOException {
        super(config);
    }

    @Override
    public boolean isWebSocketURI(String uri) {
        return uri.equals("/echo");
    }

    @Override
    public void onMessage(WebSocketSession session, TextMessage message) throws IOException {
        session.sendMessage(new TextMessage(message.payload()));
    }

    @Override
    protected void handleMessage(WebSocketSession session, Message message) throws IOException {
        log.info(message);
        super.handleMessage(session, message);
    }

    public static void main(String[] args) throws IOException {
        EchoServer server = new EchoServer(config());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }

    private static HttpServerConfig config() {
        HttpServerConfig config = new HttpServerConfig();
        config.keepAlive = 30000;
        config.maxWorkers = 1000;
        config.queueTime = 50;
        config.acceptors = acceptors();
        return config;
    }

    private static AcceptorConfig[] acceptors() {
        AcceptorConfig config = new AcceptorConfig();
        config.port = 8000;
        config.backlog = 10000;
        config.deferAccept = true;
        return new AcceptorConfig[] {
                config
        };
    }

}
