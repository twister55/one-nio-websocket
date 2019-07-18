package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class PingMessage extends BinaryMessage {

    public PingMessage(byte[] payload) {
        super(WebSocketOpcode.PING, payload);
    }

}
