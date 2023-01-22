package one.nio.ws;

import java.io.IOException;

import one.nio.http.HttpServer;
import one.nio.net.Socket;
import one.nio.ws.proto.message.BinaryMessage;
import one.nio.ws.proto.message.CloseMessage;
import one.nio.ws.proto.message.PingMessage;
import one.nio.ws.proto.message.PongMessage;
import one.nio.ws.proto.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketServer extends HttpServer {
    private final WebSocketServerConfig config;

    public WebSocketServer(WebSocketServerConfig config, Object... routers) throws IOException {
        super(config, routers);
        this.config = config;
    }

    @Override
    public WebSocketSession createSession(Socket socket) {
        return new WebSocketSession(socket, this, config);
    }

    public void handleMessage(WebSocketSession session, PingMessage message) throws IOException {
        session.sendMessage(PongMessage.EMPTY);
    }

    public void handleMessage(WebSocketSession session, PongMessage message) throws IOException {
        // nothing by default
    }

    public void handleMessage(WebSocketSession session, TextMessage message) throws IOException {
        // nothing by default
    }

    public void handleMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        // nothing by default
    }

    public void handleMessage(WebSocketSession session, CloseMessage message) throws IOException {
        session.close(CloseMessage.NORMAL);
    }
}
