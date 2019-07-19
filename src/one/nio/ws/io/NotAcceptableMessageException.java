package one.nio.ws.io;

import one.nio.ws.message.CloseMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class NotAcceptableMessageException extends ProtocolException {

    public NotAcceptableMessageException(String message) {
        super(CloseMessage.CANNOT_ACCEPT, "can not accept " + message);
    }

}
