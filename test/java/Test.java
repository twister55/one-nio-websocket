import java.io.IOException;

import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import one.nio.ws.WebSocketServer;

public class Test {

    public static void main(String[] args) throws IOException {
        WebSocketServer server = new WebSocketServer(config());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }

    private static HttpServerConfig config() {
        HttpServerConfig config = new HttpServerConfig();
        config.keepAlive = 30000;
        config.maxWorkers = 1000;
        config.queueTime = 50;
        config.acceptors = acceptors();
        return config;
    }

    private static AcceptorConfig[] acceptors() {
        AcceptorConfig config = new AcceptorConfig();
        config.port = 8000;
        config.backlog = 10000;
        config.deferAccept = true;
        return new AcceptorConfig[] {
                config
        };
    }

}
