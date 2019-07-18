package one.nio.ws;

import java.io.IOException;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.ws.exception.WebSocketException;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.WebSocketMessage;
import one.nio.ws.message.WebSocketMessageReader;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketSession extends HttpSession {

    private final WebSocketServer server;
    private final WebSocketMessageReader reader;

    protected volatile boolean upgraded;

    public WebSocketSession(Socket socket, WebSocketServer server) {
        super(socket, server);
        this.server = server;
        this.reader = new WebSocketMessageReader(this);
    }

    public void upgrade(Request request) throws IOException {
        final String version = request.getHeader("Sec-WebSocket-Version: ");

        if (!"13".equals(version)) {
            Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
            response.addHeader("Sec-WebSocket-Version: 13");
            sendResponse(response);
        } else {
            Response response = new Response(Response.SWITCHING_PROTOCOLS, Response.EMPTY);
            response.addHeader("Upgrade: websocket");
            response.addHeader("Connection: Upgrade");
            response.addHeader("Sec-WebSocket-Accept: " + WebSocketUpgradeUtil.getHash(request));
            sendResponse(response);
            this.upgraded = true;
        }
    }

    @Override
    public int checkStatus(long currentTime, long keepAlive) {
        if (currentTime - lastAccessTime < keepAlive) {
            return ACTIVE;
        }

        try {
            if (wasSelected) {
                write(PingMessage.FRAME);
            }

            return ACTIVE;
        } catch (IOException e) {
            return STALE;
        }
    }

    public void sendMessage(WebSocketMessage message) throws IOException {
        write(message.serialize());
    }

    public void close(short code) {
        try {
            sendMessage(new CloseMessage(code));
        } catch (Exception e) {
            log.warn("Error while sending closing frame. Closing will be not clean", e);
        } finally {
            close();
        }
    }

    @Override
    protected void processRead(byte[] buffer) throws IOException {
        if (!upgraded) {
            super.processRead(buffer);
        } else {
            final WebSocketMessage message = this.reader.read();

            if (message != null) {
                server.handleMessage(this, message);
            }
        }
    }

    @Override
    public void handleException(Throwable e) {
        if (e instanceof WebSocketException) {
            if (log.isDebugEnabled()) {
                log.debug(e);
            }

            close(((WebSocketException) e).code());
            return;
        }

        super.handleException(e);
    }

    void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

}
