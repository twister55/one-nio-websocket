package one.nio.ws.io;

import one.nio.ws.message.BinaryMessage;
import one.nio.ws.message.CloseMessage;
import one.nio.ws.message.Message;
import one.nio.ws.message.PingMessage;
import one.nio.ws.message.PongMessage;
import one.nio.ws.message.TextMessage;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
class Frame {
    boolean fin;
    int rsv;
    Opcode opcode;
    boolean masked;
    int payloadLength;
    byte[] mask;
    byte[] payload;

    Frame(byte[] header) throws ProtocolException {
        byte b0 = header[0];
        byte b1 = header[1];

        fin = (b0 & 0x80) > 0;
        rsv = (b0 & 0x70) >>> 4;
        opcode = Opcode.valueOf(b0 & 0x0F);
        masked = (b1 & 0x80) != 0;
        payloadLength = b1 & 0x7F;

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

    Message toMessage() {
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
