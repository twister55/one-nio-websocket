package one.nio.ws.proto.message;

import one.nio.util.Hex;
import one.nio.ws.proto.Opcode;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class BinaryMessage extends Message<byte[]> {

    public BinaryMessage(byte[] payload) {
        this(Opcode.BINARY, payload);
    }

    protected BinaryMessage(Opcode opcode, byte[] payload) {
        super(opcode, payload);
    }

    @Override
    public byte[] payload() {
        return payload;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + Hex.toHex(payload) + ">";
    }
}
