package one.nio.ws.io;

import java.io.IOException;

import one.nio.net.Session;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class FrameReader {
    private static final int FIRST_HEADER_LENGTH = 2;
    private static final int MASK_LENGTH = 4;

    private final Session session;

    private Frame frame;
    private int ptr;
    private byte[] header;

    public FrameReader(Session session) {
        this.session = session;
        this.header = new byte[10];
    }

    public Frame read() throws IOException {
        Frame frame = this.frame;
        int ptr = this.ptr;

        if (frame == null) {
            ptr += session.read(header, ptr, FIRST_HEADER_LENGTH - ptr);

            if (ptr < FIRST_HEADER_LENGTH) {
                this.ptr = ptr;
                return null;
            }

            frame = createFrame(header);
            ptr = 0;
            this.frame = frame;
        }

        if (frame.payload == null) {
            int len = frame.getPayloadLength() == 126 ? 2 : frame.getPayloadLength() == 127 ? 8 : 0;
            int payloadLength = frame.getPayloadLength();

            if (len > 0) {
                ptr += session.read(header, ptr, len - ptr);

                if (ptr < len) {
                    this.ptr = ptr;
                    return null;
                }

                payloadLength = byteArrayToInt(header, len);
            }

            frame.setPayloadLength(payloadLength);
            frame.setPayload(new byte[payloadLength]);
            ptr = 0;
        }

        if (frame.mask == null) {
            ptr += session.read(header, ptr, MASK_LENGTH - ptr);

            if (ptr < MASK_LENGTH) {
                this.ptr = ptr;
                return null;
            }

            frame.mask = new byte[MASK_LENGTH];
            System.arraycopy(header, 0, frame.mask, 0, MASK_LENGTH);
            ptr = 0;
        }

        if (ptr < frame.getPayloadLength()) {
            ptr += session.read(frame.payload, ptr, frame.getPayloadLength() - ptr);

            if (ptr < frame.getPayloadLength()) {
                this.frame = frame;
                this.ptr = ptr;
                return null;
            }
        }

        this.frame = null;
        this.ptr = 0;

        return frame;
    }

    private Frame createFrame(byte[] header) throws ProtocolException {
        byte b0 = header[0];
        byte b1 = header[1];

        boolean fin = (b0 & 0x80) > 0;
        int rsv = (b0 & 0x70) >>> 4;
        Opcode opcode = Opcode.valueOf(b0 & 0x0F);
        boolean masked = (b1 & 0x80) != 0;
        int payloadLength = b1 & 0x7F;

        if (rsv != 0) {
            throw new ProtocolException("wrong rsv - " + rsv);
        }

        if (opcode == null) {
            throw new NotAcceptableMessageException("invalid opcode (" + (b0 & 0x0F) + ')');
        } else if (opcode.isControl()) {
            if (payloadLength > 125) {
                throw new ProtocolException("control payload too big");
            }

            if (!fin) {
                throw new ProtocolException("control payload can not be fragmented");
            }
        }

        if (!masked) {
            throw new ProtocolException("not masked");
        }

        return new Frame(fin, rsv, opcode, masked, payloadLength);
    }

    private int byteArrayToInt(byte[] b, int len) {
        int result = 0;
        int shift = 0;

        for (int i = len - 1; i >= 0; i--) {
            result = result + ((b[i] & 0xFF) << shift);
            shift += 8;
        }

        return result;
    }

}
