package one.nio.ws.handshake;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketVersionException extends WebSocketHandshakeException {

    public WebSocketVersionException(String version) {
        super("Unsupported websocket version " + version);
    }

}
