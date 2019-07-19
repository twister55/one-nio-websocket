package one.nio.ws.io;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
class Frame {
    private boolean fin;
    private int rsv;
    private Opcode opcode;
    private boolean masked;
    private int payloadLength;
    public byte[] mask;
    public byte[] payload;

    public Frame(boolean fin, int rsv, Opcode opcode, boolean masked, int payloadLength) {
        this.fin = fin;
        this.rsv = rsv;
        this.opcode = opcode;
        this.masked = masked;
        this.payloadLength = payloadLength;
    }

    public boolean isFin() {
        return fin;
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

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    byte[] getUnmaskedPayload() {
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

}
