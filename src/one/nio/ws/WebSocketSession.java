package one.nio.ws;

import java.io.IOException;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.WebSocketMessageReader;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketSession extends HttpSession {
    private final WebSocketServer server;
    private final WebSocketHandshaker handshaker;
    private final WebSocketMessageReader reader;

    public WebSocketSession(Socket socket, WebSocketServer server) {
        super(socket, server);
        this.server = server;
        this.handshaker = new WebSocketHandshaker(this);
        this.reader = new WebSocketMessageReader(this);
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

    public void sendMessage(Message message) throws IOException {
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

    void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    protected void processRead(byte[] buffer) throws IOException {
        if (!handshaker.isUpgraded()) {
            super.processRead(buffer);
        } else {
            final Message message = this.reader.read();

            if (message != null) {
                server.handleMessage(this, message);
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

    protected void handshake(Request request) throws IOException {
        try {
            handshaker.handshake(request);
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
