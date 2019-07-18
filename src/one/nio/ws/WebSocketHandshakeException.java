package one.nio.ws;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketHandshakeException extends RuntimeException {

    public WebSocketHandshakeException(String s) {
        super(s);
    }

    public WebSocketHandshakeException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
