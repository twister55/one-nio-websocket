package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class NotAcceptableMessageException extends WebSocketProtocolException  {

    public NotAcceptableMessageException(String message) {
        super(CloseMessage.CANNOT_ACCEPT, "can not accept " + message);
    }

}
