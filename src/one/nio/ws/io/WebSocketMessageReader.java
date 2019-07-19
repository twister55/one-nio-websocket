package one.nio.ws.io;

import java.io.IOException;

import one.nio.net.Session;
import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.PongMessage;
import one.nio.ws.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketMessageReader {

    private FrameReader reader;
    private MessageAggregator aggregator;

    public WebSocketMessageReader(Session session) {
        this.reader = new FrameReader(session);
    }

    public Message read() throws IOException {
        final Frame frame = reader.read();

        if (frame == null) {
            // not all frame data was read
            return null;
        }

        if (frame.isControl()) {
            // control messages can not be fragmented
            // and it can be between 2 fragments of another message
            // so handle it separately
            return createMessage(frame);
        }

        if (!frame.isFin()) {
            saveFragment(frame); // not finished fragmented frame
            return null;
        }

        return aggregator != null ? collectFragments(frame) : createMessage(frame);
    }

    private void saveFragment(Frame frame) throws IOException {
        if (aggregator == null) {
            aggregator = new MessageAggregator(frame.getOpcode());
        }

        aggregator.append(frame);
    }

    private Message collectFragments(Frame frame) throws IOException {
        aggregator.append(frame);
        final Message<?> message = createMessage(aggregator.getOpcode(), aggregator.getPayload());
        aggregator = null;
        return message;
    }

    private Message createMessage(Frame frame) {
        return createMessage(frame.getOpcode(), frame.getUnmaskedPayload());
    }

    private Message createMessage(Opcode opcode, byte[] payload) {
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
}
