package one.nio.ws.proto;

import one.nio.ws.proto.message.CloseMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class NotAcceptableMessageException extends ProtocolException {

    public NotAcceptableMessageException(String message) {
        super(CloseMessage.CANNOT_ACCEPT, "can not accept " + message);
    }

}
