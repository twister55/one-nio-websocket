package one.nio.ws.io;

import java.io.IOException;
import java.util.List;

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
public class WebSocketMessageWriter {

    private final Session session;
    private final List<Extension> extensions;

    public WebSocketMessageWriter(Session session, List<Extension> extensions) {
        this.session = session;
        this.extensions = extensions;
    }

    public void write(Message message) throws IOException {
        final Frame frame = createFrame(message);
        final byte[] payload = frame.getPayload();
        final byte[] header = serializeHeader(frame.getRsv(), frame.getOpcode(), payload);

        session.write(header, 0, header.length);
        session.write(payload, 0, payload.length);
    }

    private Frame createFrame(Message message) throws IOException {
        Frame frame = new Frame(getOpcode(message), message.bytesPayload());

        for (Extension extension : extensions) {
            extension.transformOutput(frame);
        }

        return frame;
    }

    private Opcode getOpcode(Message message) {
        if (message instanceof PingMessage) {
            return Opcode.PING;
        } else if (message instanceof PongMessage) {
            return Opcode.PONG;
        } else if (message instanceof TextMessage) {
            return Opcode.TEXT;
        } else if (message instanceof BinaryMessage) {
            return Opcode.BINARY;
        } else if (message instanceof CloseMessage) {
            return Opcode.CLOSE;
        }

        throw new IllegalArgumentException("Unsupported opcode for " + message.getClass().getSimpleName());
    }

    private byte[] serializeHeader(int rsv, Opcode opcode, byte[] payload) {
        int len = payload.length < 126 ? 2 : payload.length < 65536 ? 4 : 10;
        byte[] header = new byte[len];
        byte b = 0;

        b -= 128; // set the fin bit
        b += (rsv << 4);
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
