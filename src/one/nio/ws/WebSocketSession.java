package one.nio.ws;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.ws.io.WebSocketMessageReader;
import one.nio.ws.io.WebSocketMessageWriter;
import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.PongMessage;
import one.nio.ws.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketSession extends HttpSession {
    protected static final Log log = LogFactory.getLog(WebSocketSession.class);

    private final WebSocketServer server;
    private final WebSocketHandshaker handshaker;

    private WebSocketMessageReader reader;
    private WebSocketMessageWriter writer;

    public WebSocketSession(Socket socket, WebSocketServer server) {
        this(socket, server, new WebSocketHandshaker());
    }

    public WebSocketSession(Socket socket, WebSocketServer server, WebSocketHandshaker handshaker) {
        super(socket, server);
        this.server = server;
        this.handshaker = handshaker;
    }

    @Override
    public int checkStatus(long currentTime, long keepAlive) {
        if (currentTime - lastAccessTime < keepAlive) {
            return ACTIVE;
        }

        try {
            if (wasSelected) {
                sendMessage(PingMessage.EMPTY);
            }

            return ACTIVE;
        } catch (IOException e) {
            return STALE;
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("Web socket message was sent before handshake");
        }

        writer.write(message);
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
        if (reader == null) {
            super.processRead(buffer);
        } else {
            final Message message = reader.read();

            if (message != null) {
                handleMessage(this, message);
            }
        }
    }

    /*
     * FIXME нужен отдельный метод в HttpSession чтобы не копипастить
     *  protected void handleRequest(Request request) {
     *      server.handleRequest(request, this);
     *  }
     */
    @Override
    protected void handleParsedRequest() throws IOException {
        if (handling == null) {
            handleRequest(handling = parsing);
        } else if (pipeline.size() < 256) {
            pipeline.addLast(parsing);
        } else {
            throw new IOException("Pipeline length exceeded");
        }
        parsing = null;
        requestBodyOffset = 0;
    }

    protected void handleRequest(Request request) throws IOException {
        if (server.isWebSocketURI(request.getURI())) {
            handshake(request);
            return;
        }

        server.handleRequest(request, this);
    }

    protected void handleMessage(WebSocketSession session, Message message) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }

        if (message instanceof PingMessage) {
            server.handleMessage(session, (PingMessage) message);
        } else if (message instanceof PongMessage) {
            server.handleMessage(session, (PongMessage) message);
        } else if (message instanceof TextMessage) {
            server.handleMessage(session, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            server.handleMessage(session, (BinaryMessage) message);
        } else if (message instanceof CloseMessage) {
            server.handleMessage(session, (CloseMessage) message);
        }
    }

    protected void handshake(Request request) throws IOException {
        try {
            handshaker.handshake(this, request);

            reader = new WebSocketMessageReader(this);
            writer = new WebSocketMessageWriter(this);
        } catch (WebSocketVersionException e) {
            Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
            response.addHeader("Sec-WebSocket-Version: 13");
            response.addHeader("Content-Length: 0");
            sendResponse(response);
        } catch (WebSocketHandshakeException e) {
            sendError(Response.BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public void handleException(Throwable e) {
        if (e instanceof WebSocketException) {
            handleWebSocketException((WebSocketException) e);
            return;
        }

        super.handleException(e);
    }

    protected void handleWebSocketException(WebSocketException e) {
        close(e.code());
    }

}
