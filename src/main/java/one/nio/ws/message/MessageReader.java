package one.nio.ws.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import one.nio.net.Session;
import one.nio.ws.exception.TooBigException;
import one.nio.ws.exception.WebSocketException;
import one.nio.ws.frame.Frame;
import one.nio.ws.frame.FrameReader;
import one.nio.ws.frame.Opcode;
import one.nio.ws.extension.Extension;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class MessageReader {
    private final FrameReader reader;
    private final List<Extension> extensions;
    private final int maxMessagePayloadLength;

    private PayloadBuffer buffer;

    public MessageReader(Session session,
                         List<Extension> extensions,
                         int maxFramePayloadLength,
                         int maxMessagePayloadLength) {
        this.reader = new FrameReader(session, maxFramePayloadLength);
        this.extensions = extensions;
        this.maxMessagePayloadLength = maxMessagePayloadLength;
    }

    public Message<?> read() throws IOException {
        final Frame frame = reader.read();
        if (frame == null) {
            // not all frame data was transformInput
            return null;
        }
        if (frame.isControl()) {
            // control messages can not be fragmented
            // and it can be between 2 fragments of another message
            // so handle it separately
            return createMessage(frame.getOpcode(), getPayload(frame));
        }
        if (!frame.isFin()) {
            // not finished fragmented frame
            appendFrame(frame);
            return null;
        }
        if (buffer != null) {
            appendFrame(frame);
            Message<?> message = createMessage(buffer.getOpcode(), buffer.getPayload());
            buffer = null;
            return message;
        }
        return createMessage(frame.getOpcode(), getPayload(frame));
    }

    private void appendFrame(Frame frame) throws IOException {
        if (buffer == null) {
            buffer = new PayloadBuffer(frame.getOpcode(), maxMessagePayloadLength);
        }
        buffer.append(getPayload(frame));
    }

    private Message<?> createMessage(Opcode opcode, byte[] payload) {
        switch (opcode) {
            case CLOSE:
                return new CloseMessage(payload);
            case PING:
                return new PingMessage(payload);
            case PONG:
                return new PongMessage(payload);
            case BINARY:
                return new BinaryMessage(payload);
            case TEXT:
                return new TextMessage(payload);
        }
        throw new IllegalArgumentException("Unsupported opcode: " + opcode);
    }

    private byte[] getPayload(Frame frame) throws IOException {
        frame.unmask();
        for (Extension extension : extensions) {
            extension.transformInput(frame);
        }
        return frame.getPayload();
    }

    public static class PayloadBuffer {
        private final Opcode opcode;
        private final List<byte[]> chunks;
        private final int maxMessagePayloadLength;
        private int payloadLength;

        public PayloadBuffer(Opcode opcode, int maxMessagePayloadLength) {
            this.opcode = opcode;
            this.chunks = new ArrayList<>();
            this.maxMessagePayloadLength = maxMessagePayloadLength;
        }

        public Opcode getOpcode() {
            return opcode;
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

        public void append(byte[] payload) throws WebSocketException {
            payloadLength += payload.length;
            if (payloadLength > this.maxMessagePayloadLength) {
                throw new TooBigException("payload can not be more than " + maxMessagePayloadLength);
            }
            chunks.add(payload);
        }
    }
}
