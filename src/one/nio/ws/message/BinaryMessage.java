package one.nio.ws.message;

import one.nio.util.Hex;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class BinaryMessage extends Message<byte[]> {

    public BinaryMessage(byte[] payload) {
        super(payload);
    }

    @Override
    public byte[] bytesPayload() {
        return payload();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + Hex.toHex(payload()) + ">";
    }
}
