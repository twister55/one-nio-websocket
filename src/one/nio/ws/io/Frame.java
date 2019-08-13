package one.nio.ws.io;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class Frame {
    private boolean fin;
    private int rsv;
    private Opcode opcode;
    private int payloadLength;
    private byte[] mask;
    private byte[] payload;

    public Frame(boolean fin, int rsv, Opcode opcode, int payloadLength) {
        this.fin = fin;
        this.rsv = rsv;
        this.opcode = opcode;
        this.payloadLength = payloadLength;
    }

    public Frame(Opcode opcode, byte[] payload) {
        this.fin = true;
        this.rsv = 0;
        this.opcode = opcode;
        this.payload = payload;
        this.payloadLength = payload.length;
    }

    public boolean isFin() {
        return fin;
    }

    public int getRsv() {
        return rsv;
    }

    public void setRsv(int rsv) {
        this.rsv = rsv;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public boolean isControl() {
        return opcode.isControl();
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public byte[] getMask() {
        return mask;
    }

    public void setMask(byte[] mask) {
        this.mask = mask;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
        this.payloadLength = payload.length;
    }

    public void unmask() {
        if (mask != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }

            mask = null;
        }
    }

}
