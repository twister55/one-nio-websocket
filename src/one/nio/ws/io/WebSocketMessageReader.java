package one.nio.ws.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import one.nio.net.Session;
import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.TextMessage;

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

    public Message read() throws IOException {
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

    private Message collectFragments(Frame lastFrame) {
        fragmentedFrame.add(lastFrame);
        final Message<?> message = fragmentedFrame.toMessage();
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

    private static class FragmentedFrame {
        Opcode opcode;
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

        Message<?> toMessage() {
            byte[] payload = new byte[payloadLength];

            for (Frame fragment : fragments) {
                System.arraycopy(fragment.payload(), 0, payload, 0, fragment.payloadLength);
            }

            return opcode == Opcode.BINARY ? new BinaryMessage(payload) : new TextMessage(payload);
        }
    }
}
