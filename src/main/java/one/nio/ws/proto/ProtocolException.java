package one.nio.ws.proto;

import one.nio.ws.WebSocketException;
import one.nio.ws.proto.message.CloseMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class ProtocolException extends WebSocketException {

    public ProtocolException(String message) {
        this(CloseMessage.PROTOCOL_ERROR, message);
    }

    protected ProtocolException(short code, String message) {
        super(code, "Web socket protocol error: " + message);
    }
}
