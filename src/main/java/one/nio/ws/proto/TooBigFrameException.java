package one.nio.ws.proto;

import one.nio.ws.proto.message.CloseMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class TooBigFrameException extends ProtocolException {

    public TooBigFrameException(String message) {
        super(CloseMessage.TOO_BIG, message);
    }
}
