package one.nio.ws.message;

import one.nio.http.Response;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class PingMessage extends BinaryMessage {
    public static final byte[] FRAME = new PingMessage(Response.EMPTY).serialize();

    public PingMessage(byte[] payload) {
        super(WebSocketOpcode.PING, payload);
    }

}
