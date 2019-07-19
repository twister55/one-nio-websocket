package one.nio.ws.message;

/**
 * @author <a href="mailto:vadim.yelisseyev@gmail.com">Vadim Yelisseyev</a>
 */
public abstract class Message<T> {

    private final T payload;

    protected Message(T payload) {
        this.payload = payload;
    }

    public T payload() {
        return payload;
    }

    public abstract byte[] bytesPayload();

}
