package one.nio.ws.proto.message;

import java.io.IOException;
import java.util.List;

import one.nio.net.Session;
import one.nio.net.Socket;
import one.nio.ws.proto.Frame;
import one.nio.ws.proto.Opcode;
import one.nio.ws.proto.extension.Extension;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class MessageWriter {
    private final Session session;
    private final List<Extension> extensions;

    public MessageWriter(Session session, List<Extension> extensions) {
        this.session = session;
        this.extensions = extensions;
    }

    public void write(Message<?> message) throws IOException {
        final Frame frame = createFrame(message);
        final byte[] payload = frame.getPayload();
        final byte[] header = serializeHeader(frame.getRsv(), frame.getOpcode(), payload);
        session.write(header, 0, header.length, Socket.MSG_MORE);
        session.write(payload, 0, payload.length);
    }

    private Frame createFrame(Message<?> message) throws IOException {
        Frame frame = new Frame(message.opcode(), message.payload());
        for (Extension extension : extensions) {
            extension.transformOutput(frame);
        }
        return frame;
    }

    private byte[] serializeHeader(int rsv, Opcode opcode, byte[] payload) {
        int len = payload.length < 126 ? 2 : payload.length < 65536 ? 4 : 10;
        byte[] header = new byte[len];
        header[0] = (byte) (0x80 | (rsv << 4) | opcode.value);
        // Next write the mask && length length
        if (payload.length < 126) {
            header[1] = (byte) (payload.length);
        } else if (payload.length < 65536) {
            header[1] = (byte) 126;
            header[2] = (byte) (payload.length >>> 8);
            header[3] = (byte) (payload.length & 0xFF);
        } else {
            // Will never be more than 2^31-1
            header[1] = (byte) 127;
            header[6] = (byte) (payload.length >>> 24);
            header[7] = (byte) (payload.length >>> 16);
            header[8] = (byte) (payload.length >>> 8);
            header[9] = (byte) payload.length;
        }
        return header;
    }
}
