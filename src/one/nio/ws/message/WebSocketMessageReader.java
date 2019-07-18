package one.nio.ws.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import one.nio.net.Session;
import one.nio.ws.exception.NotAcceptableMessageException;
import one.nio.ws.exception.WebSocketProtocolException;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketMessageReader {
    private static final int FIRST_HEADER_LENGTH = 2;
    private static final int MASK_LENGTH = 4;

    private final Session session;

    private Frame frame;
    private FragmentedFrame fragmentedFrame;
    private int ptr;
    private byte[] header;

    public WebSocketMessageReader(Session session) {
        this.session = session;
        this.header = new byte[10];
    }

    public WebSocketMessage read() throws IOException {
        final Frame frame = readFrame();

        if (frame == null) {
            return null;
        }

        if (frame.opcode.isControl()) {
            return frame.toMessage();
        }

        if (!frame.fin) {
            addFragment(frame); // not finished fragmented frame
            return null;
        }

        if (fragmentedFrame != null) {
            return collectFragments(frame);
        }

        return frame.toMessage();
    }

    private Frame readFrame() throws IOException {
        Frame frame = this.frame;
        int ptr = this.ptr;

        if (frame == null) {
            ptr += session.read(header, ptr, FIRST_HEADER_LENGTH - ptr);

            if (ptr < FIRST_HEADER_LENGTH) {
                this.ptr = ptr;
                return null;
            }

            frame = new Frame(header);
            ptr = 0;
            this.frame = frame;
        }

        if (frame.payload == null) {
            int len = frame.payloadLength == 126 ? 2 : frame.payloadLength == 127 ? 8 : 0;

            if (len > 0) {
                ptr += session.read(header, ptr, len - ptr);

                if (ptr < len) {
                    this.ptr = ptr;
                    return null;
                }

                frame.payloadLength = byteArrayToInt(header, len);
            }

            frame.payload = new byte[frame.payloadLength];
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

        if (ptr < frame.payloadLength) {
            ptr += session.read(frame.payload, ptr, frame.payloadLength - ptr);

            if (ptr < frame.payloadLength) {
                this.frame = frame;
                this.ptr = ptr;
                return null;
            }
        }

        this.frame = null;
        this.ptr = 0;

        return frame;
    }

    private void addFragment(Frame frame) {
        if (fragmentedFrame == null) {
            fragmentedFrame = new FragmentedFrame(frame);
        } else {
            fragmentedFrame.add(frame);
        }
    }

    private WebSocketMessage collectFragments(Frame lastFrame) {
        fragmentedFrame.add(lastFrame);
        final WebSocketMessage<?> message = fragmentedFrame.toMessage();
        fragmentedFrame = null;
        return message;
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

    private static class Frame {
        boolean fin;
        int rsv;
        WebSocketOpcode opcode;
        boolean masked;
        int payloadLength;
        byte[] mask;
        byte[] payload;

        Frame(byte[] header) throws WebSocketProtocolException {
            byte b0 = header[0];
            byte b1 = header[1];

            fin = (b0 & 0x80) > 0;
            rsv = (b0 & 0x70) >>> 4;
            opcode = WebSocketOpcode.valueOf(b0 & 0x0F);
            masked = (b1 & 0x80) != 0;
            payloadLength = b1 & 0x7F;

            if (rsv != 0) {
                throw new WebSocketProtocolException("wrong rsv - " + rsv);
            }

            if (opcode == null) {
                throw new NotAcceptableMessageException("invalid opcode (" + (b0 & 0x0F) + ')');
            } else if (opcode.isControl()) {
                if (payloadLength > 125) {
                    throw new WebSocketProtocolException("control payload too big");
                }

                if (!fin) {
                    throw new WebSocketProtocolException("control payload can not be fragmented");
                }
            }

            if (!masked) {
                throw new WebSocketProtocolException("not masked");
            }
        }

        byte[] payload() {
            if (mask == null) {
                return payload;
            }

            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }

            mask = null;
            masked = false;

            return payload;
        }

        WebSocketMessage toMessage() {
            switch (opcode) {
                case CLOSE:
                    return new CloseMessage(payload());

                case PING:
                    return new PingMessage(payload());

                case PONG:
                    return new PongMessage(payload());

                case BINARY:
                    return new BinaryMessage(payload());

                default:
                    return new TextMessage(payload());
            }
        }
    }

    private static class FragmentedFrame {
        WebSocketOpcode opcode;
        final List<Frame> fragments;
        int payloadLength;

        FragmentedFrame(Frame frame) {
            this.fragments = new ArrayList<>();
            this.fragments.add(frame);
            this.opcode = frame.opcode;
            this.payloadLength = frame.payloadLength;
        }

        void add(Frame frame) {
            fragments.add(frame);
            payloadLength += frame.payloadLength;
        }

        WebSocketMessage<?> toMessage() {
            byte[] payload = new byte[payloadLength];

            for (Frame fragment : fragments) {
                System.arraycopy(fragment.payload(), 0, payload, 0, fragment.payloadLength);
            }

            return opcode == WebSocketOpcode.BINARY ? new BinaryMessage(payload) : new TextMessage(payload);
        }
    }
}
