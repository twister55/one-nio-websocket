package one.nio.ws;

import one.nio.http.HttpServerConfig;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public class WebSocketServerConfig extends HttpServerConfig {
    public String websocketBaseUri = "/";
    public int maxFramePayloadLength = 65536;
    public int maxMessagePayloadLength = 16 * 1024 * 1024;
}
