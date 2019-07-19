package one.nio.ws.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class MessageAggregator {
    private final Opcode opcode;
    private final ByteArrayOutputStream stream;

    public MessageAggregator(Opcode opcode) {
        this.opcode = opcode;
        this.stream = new ByteArrayOutputStream();
    }

    public void append(Frame frame) throws IOException {
        stream.write(frame.getUnmaskedPayload());
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public byte[] getPayload() throws IOException {
        final byte[] result = stream.toByteArray();

        stream.close();

        return result;
    }
}
