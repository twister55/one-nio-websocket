package one.nio.ws.proto.message;

import java.util.ArrayList;
import java.util.List;

import one.nio.ws.proto.Opcode;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class MessageAggregator {
    private final Opcode opcode;
    private final List<byte[]> chunks;
    private int payloadLength;

    public MessageAggregator(Opcode opcode) {
        this.opcode = opcode;
        this.chunks = new ArrayList<>();
    }

    public void append(byte[] payload) {
        chunks.add(payload);
        payloadLength += payload.length;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public byte[] getPayload() {
        final byte[] result = new byte[payloadLength];
        int pos = 0;
        for (byte[] chunk : chunks) {
            int length = chunk.length;
            System.arraycopy(chunk,0, result, pos, length);
            pos += length;
        }
        return result;
    }
}
