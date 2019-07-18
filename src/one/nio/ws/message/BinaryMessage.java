package one.nio.ws.message;

import java.util.Arrays;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class BinaryMessage extends Message<byte[]> {

    protected BinaryMessage(WebSocketOpcode opcode, byte[] payload) {
        super(opcode, payload);
    }

    public BinaryMessage(byte[] payload) {
        super(WebSocketOpcode.BINARY, payload);
    }

    @Override
    protected byte[] payloadAsBytes() {
        return payload();
    }

    @Override
    protected String payloadAsString() {
        return Arrays.toString(payload());
    }
}
