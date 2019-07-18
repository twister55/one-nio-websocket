package one.nio.ws.message;

import static one.nio.util.JavaInternals.byteArrayOffset;
import static one.nio.util.JavaInternals.unsafe;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class CloseMessage extends WebSocketMessage<Short> {
    public static short NORMAL = 1000;
    public static short GOING_AWAY = 1001;
    public static short PROTOCOL_ERROR = 1002;
    public static short CANNOT_ACCEPT = 1003;
    public static short RESERVED = 1004;
    public static short NO_STATUS_CODE = 1005;
    public static short CLOSED_ABNORMALLY = 1006;
    public static short NOT_CONSISTENT = 1007;
    public static short VIOLATED_POLICY = 1008;
    public static short TOO_BIG = 1009;
    public static short NO_EXTENSION = 1010;
    public static short UNEXPECTED_CONDITION = 1011;
    public static short SERVICE_RESTART = 1012;
    public static short TRY_AGAIN_LATER = 1013;
    public static short TLS_HANDSHAKE_FAILURE = 1015;

    public CloseMessage(byte[] payload) {
        this(Short.reverseBytes(unsafe.getShort(payload, byteArrayOffset)));
    }

    public CloseMessage(short code) {
        super(WebSocketOpcode.CLOSE, code);
    }

    @Override
    public byte[] payloadAsBytes() {
        final byte[] result = new byte[2];
        unsafe.putShort(result, byteArrayOffset, Short.reverseBytes(payload()));
        return result;
    }

    @Override
    protected String payloadAsString() {
        return String.valueOf(payload());
    }

}

