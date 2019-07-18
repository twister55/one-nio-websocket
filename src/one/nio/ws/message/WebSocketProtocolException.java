package one.nio.ws.message;

import one.nio.ws.WebSocketException;
import one.nio.ws.message.CloseMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketProtocolException extends WebSocketException {

    public WebSocketProtocolException(String message) {
        this(CloseMessage.PROTOCOL_ERROR, message);
    }

    protected WebSocketProtocolException(short code, String message) {
        super(code, "Web socket protocol error: " + message);
    }

}
