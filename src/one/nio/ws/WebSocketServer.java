package one.nio.ws;

import java.io.IOException;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.net.Socket;
import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.PongMessage;
import one.nio.ws.message.TextMessage;
import one.nio.ws.message.WebSocketMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketServer extends HttpServer {

    public WebSocketServer(HttpServerConfig config) throws IOException {
        super(config);
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

    @Override
    public void handleRequest(Request request, HttpSession session) throws IOException {
        if (isUpgradableRequest(request) && session instanceof WebSocketSession) {
            ((WebSocketSession) session).upgrade(request);
            return;
        }

        super.handleRequest(request, session);
    }

    protected void handleMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
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

    private boolean isUpgradableRequest(Request request) {
        final String upgradeHeader = request.getHeader("Upgrade: ");
        final String connectionHeader = request.getHeader("Connection: ");

        return upgradeHeader != null && upgradeHeader.contains("websocket") &&
                connectionHeader != null && connectionHeader.contains("Upgrade");
    }

}
