package one.nio.ws.proto.message;

import one.nio.http.Response;
import one.nio.ws.proto.Opcode;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class PingMessage extends BinaryMessage {
    public static final PingMessage EMPTY = new PingMessage(Response.EMPTY);

    public PingMessage(byte[] payload) {
        super(Opcode.PING, payload);
    }
}
