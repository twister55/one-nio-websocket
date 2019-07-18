package one.nio.ws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import one.nio.http.Request;
import one.nio.util.Base64;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketUpgradeUtil {
    private static final String ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("SHA-1 not supported on this platform");
        }
    });

    public static String getHash(Request request) throws IOException {
        String key = request.getHeader("Sec-WebSocket-Key: ");

        if (key == null) {
            throw new IOException("Invalid request: missing websocket key");
        }

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
