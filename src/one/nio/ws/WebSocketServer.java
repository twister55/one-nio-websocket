package one.nio.ws;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.net.Socket;
import one.nio.ws.message.TextMessage;
import one.nio.ws.message.WebSocketMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketServer extends HttpServer {
    private static final Log log = LogFactory.getLog(WebSocketServer.class);

    public WebSocketServer(HttpServerConfig config) throws IOException {
        super(config);
    }

    @Override
    public WebSocketSession createSession(Socket socket) {
        return new WebSocketSession(socket, this);
    }

    @Override
    public void handleRequest(Request request, HttpSession session) throws IOException {
        if (isUpgradableRequest(request) && session instanceof WebSocketSession) {
            ((WebSocketSession) session).upgrade(request);
            return;
        }

        super.handleRequest(request, session);
    }

    public void handleMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.payload();
            int length = text.length();

            if (length > 10) {
                text = text.substring(0, 10);
            }

            log.info("Accepted: TextMessage {'" + text + "}");
            session.sendMessage(new TextMessage(textMessage.payload().toUpperCase()));
        } else {
            log.info("Accepted: " + message);
        }
    }

    private boolean isUpgradableRequest(Request request) {
        final String upgradeHeader = request.getHeader("Upgrade: ");
        final String connectionHeader = request.getHeader("Connection: ");

        return upgradeHeader != null && upgradeHeader.contains("websocket") &&
                connectionHeader != null && connectionHeader.contains("Upgrade");
    }

}
