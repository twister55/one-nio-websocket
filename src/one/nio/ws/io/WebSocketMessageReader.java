package one.nio.ws.io;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.nio.net.Session;
import one.nio.ws.extension.Extension;
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
    private static final Log log = LogFactory.getLog(WebSocketMessageReader.class);

    private final FrameReader reader;
    private final List<Extension> extensions;
    private MessageAggregator aggregator;

    public WebSocketMessageReader(Session session, List<Extension> extensions) {
        this.reader = new FrameReader(session);
        this.extensions = extensions;
    }

    public Message read() throws IOException {
        final Frame frame = reader.read();

        if (frame == null) {
            // not all frame data was transformInput
            return null;
        }

        if (frame.isControl()) {
            // control messages can not be fragmented
            // and it can be between 2 fragments of another message
            // so handle it separately
            return createMessage(frame);
        }

        if (!frame.isFin()) {
            // not finished fragmented frame
            saveFragment(frame);
            return null;
        }

        if (aggregator != null) {
            saveFragment(frame);
            Message<?> message = createMessage(aggregator.getOpcode(), aggregator.getPayload());
            aggregator = null;
            return message;
        }

        return createMessage(frame);
    }

    private void saveFragment(Frame frame) throws IOException {
        if (aggregator == null) {
            aggregator = new MessageAggregator(frame.getOpcode());
        }

        aggregator.append(getPayload(frame));
    }

    private Message createMessage(Frame frame) throws IOException {
        return createMessage(frame.getOpcode(), getPayload(frame));
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

    private byte[] getPayload(Frame frame) throws IOException {
        frame.unmask();

        for (Extension extension : extensions) {
            extension.transformInput(frame);
        }

        return frame.getPayload();
    }
}
