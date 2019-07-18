package one.nio.ws.message;

import one.nio.http.Response;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class PongMessage extends BinaryMessage {
    public static final byte[] FRAME = new PongMessage(Response.EMPTY).serialize();

    public PongMessage(byte[] payload) {
        super(WebSocketOpcode.PING, payload);
    }

}

