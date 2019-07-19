package one.nio.ws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.util.Base64;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketHandshaker {
    private static final String ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("SHA-1 not supported on this platform");
        }
    });

    public void handshake(WebSocketSession session, Request request) throws IOException {
        validate(request);
        sendUpgradeResponse(session, request);
    }

    protected void validate(Request request) {
        final String version = request.getHeader("Sec-WebSocket-Version: ");

        if (!"13".equals(version)) {
            throw new WebSocketVersionException(version);
        }

        if (request.getMethod() != Request.METHOD_GET) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: only GET method supported");
        }

        if (request.getHeader("Sec-WebSocket-Key: ") == null) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: missing websocket key");
        }

        if (!isUpgradableRequest(request)) {
            throw new WebSocketHandshakeException("Not a WebSocket handshake request: missing upgrade");
        }
    }

    protected void sendUpgradeResponse(WebSocketSession session, Request request) throws IOException {
        session.sendResponse(getUpgradeResponse(request));
    }

    protected Response getUpgradeResponse(Request request) {
        Response response = new Response(Response.SWITCHING_PROTOCOLS, Response.EMPTY);
        response.addHeader("Upgrade: websocket");
        response.addHeader("Connection: Upgrade");
        response.addHeader("Sec-WebSocket-Accept: " + getHash(request));
        return response;
    }

    private boolean isUpgradableRequest(Request request) {
        final String upgradeHeader = request.getHeader("Upgrade: ");
        final String connectionHeader = request.getHeader("Connection: ");

        return upgradeHeader != null && upgradeHeader.toLowerCase().contains("websocket") &&
                connectionHeader != null && connectionHeader.toLowerCase().contains("upgrade");
    }

    private static String getHash(Request request) {
        String key = request.getHeader("Sec-WebSocket-Key: ");
        String acceptSeed = key + ACCEPT_GUID;
        byte[] sha1 = sha1(acceptSeed.getBytes(StandardCharsets.ISO_8859_1));

        return new String(Base64.encode(sha1));
    }

    private static byte[] sha1(byte[] data) {
        MessageDigest digest = SHA1.get();
        digest.reset();
        return digest.digest(data);
    }

}
