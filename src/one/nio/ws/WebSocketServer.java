package one.nio.ws;

import java.io.IOException;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.net.Socket;
import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.PongMessage;
import one.nio.ws.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketServer extends HttpServer {

    public WebSocketServer(HttpServerConfig config, Object... routers) throws IOException {
        super(config, routers);
    }

    public boolean isWebSocketURI(String uri) {
        return true;
    }

    @Override
    public WebSocketSession createSession(Socket socket) {
        return new WebSocketSession(socket, this);
    }

    public void onMessage(WebSocketSession session, PingMessage message) throws IOException {
        session.write(PongMessage.FRAME);
    }

    public void onMessage(WebSocketSession session, PongMessage message) throws IOException {
        // nothing by default
    }

    public void onMessage(WebSocketSession session, TextMessage message) throws IOException {
        // nothing by default
    }

    public void onMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        // nothing by default
    }

    public void onMessage(WebSocketSession session, CloseMessage message) throws IOException {
        session.close(CloseMessage.NORMAL);
    }

    protected void handleMessage(WebSocketSession session, Message message) throws IOException {
        if (message instanceof PingMessage) {
            onMessage(session, (PingMessage) message);
        } else if (message instanceof PongMessage) {
            onMessage(session, (PongMessage) message);
        } else if (message instanceof TextMessage) {
            onMessage(session, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            onMessage(session, (BinaryMessage) message);
        } else if (message instanceof CloseMessage) {
            onMessage(session, (CloseMessage) message);
        }
    }

}
