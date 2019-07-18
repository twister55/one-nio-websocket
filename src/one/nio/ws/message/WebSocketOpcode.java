package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
enum WebSocketOpcode {
    CONTINUATION(0x00),
    TEXT(0x01),
    BINARY(0x02),
    CLOSE(0x08),
    PING(0x09),
    PONG(0x0A);

    private static final WebSocketOpcode[] VALUES;
    static {
        VALUES = new WebSocketOpcode[11];
        for (WebSocketOpcode opcode : WebSocketOpcode.values()) {
            if (VALUES[opcode.value] != null) {
                throw new IllegalArgumentException("Opcode " + opcode.value + " already used.");
            }
            VALUES[opcode.value] = opcode;
        }
    }

    public final byte value;

    WebSocketOpcode(int value) {
        this.value = (byte) value;
    }

    public boolean isControl() {
        return (value & 0x08) > 0;
    }

    public static WebSocketOpcode valueOf(int value) {
        return VALUES[value];
    }
}
