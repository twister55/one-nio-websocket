package one.nio.ws.message;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class TextMessage extends Message<String> {

    public TextMessage(CharSequence payload) {
        this(payload.toString());
    }

    public TextMessage(byte[] payload) {
        this(new String(payload, StandardCharsets.UTF_8));
    }

    public TextMessage(String payload) {
        super(WebSocketOpcode.TEXT, payload);
    }

    @Override
    protected byte[] payloadAsBytes() {
        return payload().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected String payloadAsString() {
        return payload();
    }
}
