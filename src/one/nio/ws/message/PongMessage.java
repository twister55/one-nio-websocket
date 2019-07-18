package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class PongMessage extends BinaryMessage {

    public PongMessage(byte[] payload) {
        super(WebSocketOpcode.PING, payload);
    }

}

