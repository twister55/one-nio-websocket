package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public abstract class Message<T> {

    private final WebSocketOpcode opcode;
    private final T payload;

    protected Message(WebSocketOpcode opcode, T payload) {
        this.opcode = opcode;
        this.payload = payload;
    }

    public T payload() {
        return payload;
    }

    protected abstract byte[] payloadAsBytes();

    protected abstract String payloadAsString();

    public byte[] serialize() {
        byte[] payload = payloadAsBytes();
        byte[] header = serializeHeader(opcode, payload);
        byte[] result = new byte[header.length + payload.length];

        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(payload, 0, result, header.length, payload.length);

        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {" + payloadAsString() + '}';
    }

    private byte[] serializeHeader(WebSocketOpcode opcode, byte[] payload) {
        int len = payload.length < 126 ? 2 : payload.length < 65536 ? 4 : 10;
        byte[] header = new byte[len];
        byte b = 0;

        b -= 128; // set the fin bit
        b += opcode.value;
        header[0] = b;
        b = 0;

        // Next write the mask && length length
        if (payload.length < 126) {
            header[1] = (byte) (payload.length | b);
        } else if (payload.length < 65536) {
            header[1] = (byte) (126 | b);
            header[2] = (byte) (payload.length >>> 8);
            header[3] = (byte) (payload.length & 0xFF);
        } else {
            // Will never be more than 2^31-1
            header[1] = (byte) (127 | b);
            header[2] = (byte) 0;
            header[3] = (byte) 0;
            header[4] = (byte) 0;
            header[5] = (byte) 0;
            header[6] = (byte) (payload.length >>> 24);
            header[7] = (byte) (payload.length >>> 16);
            header[8] = (byte) (payload.length >>> 8);
            header[9] = (byte) (payload.length & 0xFF);
        }

        return header;
    }

}
