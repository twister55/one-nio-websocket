package one.nio.ws;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.ws.proto.extension.Extension;
import one.nio.ws.proto.extension.ExtensionRequest;
import one.nio.ws.proto.extension.ExtensionRequestParser;
import one.nio.ws.proto.extension.PerMessageDeflate;
import one.nio.ws.proto.message.BinaryMessage;
import one.nio.ws.proto.message.CloseMessage;
import one.nio.ws.proto.message.Message;
import one.nio.ws.proto.message.MessageReader;
import one.nio.ws.proto.message.MessageWriter;
import one.nio.ws.proto.message.PingMessage;
import one.nio.ws.proto.message.PongMessage;
import one.nio.ws.proto.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketSession extends HttpSession {
    protected static final Log log = LogFactory.getLog(WebSocketSession.class);

    private final WebSocketServer server;
    private final WebSocketServerConfig config;
    private List<Extension> extensions;
    private MessageReader reader;
    private MessageWriter writer;

    public WebSocketSession(Socket socket, WebSocketServer server, WebSocketServerConfig config) {
        super(socket, server);
        this.server = server;
        this.config = config;
    }

    public void sendMessage(Message<?> message) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("Web socket message was sent before handshake");
        }
        writer.write(message);
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

    @Override
    protected void processRead(byte[] buffer) throws IOException {
        if (reader == null) {
            super.processRead(buffer);
        } else {
            final Message<?> message = reader.read();
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

    @Override
    public void handleException(Throwable e) {
        log.error(e);
        if (e instanceof WebSocketException) {
            close(((WebSocketException) e).code());
            return;
        }
        super.handleException(e);
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
    public void close() {
        try {
            for (Extension extension : extensions) {
                extension.close();
            }
        } finally {
            super.close();
        }
    }

    protected void handleRequest(Request request) throws IOException {
        if (Objects.equals(config.websocketBaseUri, request.getURI())) {
            validate(request);
            handshake(request);
            return;
        }
        server.handleRequest(request, this);
    }

    protected void handleMessage(WebSocketSession session, Message<?> message) throws IOException {
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

    protected void validate(Request request) {
        final String version = request.getHeader(WebSocketHeaders.VERSION);
        if (!"13".equals(version)) {
            throw new WebSocketVersionException(version);
        }
        if (request.getMethod() != Request.METHOD_GET) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: only GET method supported");
        }
        if (request.getHeader(WebSocketHeaders.KEY) == null) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: missing websocket key");
        }
        if (!isUpgradableRequest(request)) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: missing upgrade");
        }
    }

    protected void handshake(Request request) throws IOException {
        try {
            final Response response = createResponse(request);
            final String extensionsHeader = request.getHeader(WebSocketHeaders.EXTENSIONS);
            final StringBuilder builder = new StringBuilder(WebSocketHeaders.EXTENSIONS);

            for (ExtensionRequest extensionRequest : ExtensionRequestParser.parse(extensionsHeader)) {
                Extension extension = createExtension(extensionRequest);
                if (extension != null) {
                    extensions.add(extension);
                    if (extensions.size() > 1) {
                        builder.append(',');
                    }
                    extension.appendResponseHeaderValue(builder);
                }
            }

            if (!extensions.isEmpty()) {
                response.addHeader(builder.toString());
            }

            reader = new MessageReader(this, extensions, config.maxFramePayloadLength, config.maxMessagePayloadLength);
            writer = new MessageWriter(this, extensions);
            sendResponse(response);
        } catch (WebSocketHandshakeException e) {
            sendError(Response.BAD_REQUEST, e.getMessage());
        }
    }

    protected Extension createExtension(ExtensionRequest request) {
        if (PerMessageDeflate.NAME.equals(request.getName())) {
            return PerMessageDeflate.negotiate(request.getParameters());
        }
        return null;
    }

    protected Response createResponse(Request request) {
        try {
            Response response = new Response(Response.SWITCHING_PROTOCOLS, Response.EMPTY);
            response.addHeader("Upgrade: websocket");
            response.addHeader("Connection: Upgrade");
            response.addHeader(WebSocketHeaders.createAcceptHeader(request));
            return response;
        } catch (WebSocketVersionException e) {
            Response response = new Response("426 Upgrade Required", Response.EMPTY);
            response.addHeader(WebSocketHeaders.createVersionHeader(13));
            return response;
        }
    }

    protected boolean isUpgradableRequest(Request request) {
        final String upgradeHeader = request.getHeader("Upgrade: ");
        final String connectionHeader = request.getHeader("Connection: ");
        return upgradeHeader != null && upgradeHeader.toLowerCase().contains("websocket") &&
                connectionHeader != null && connectionHeader.toLowerCase().contains("upgrade");
    }
}
