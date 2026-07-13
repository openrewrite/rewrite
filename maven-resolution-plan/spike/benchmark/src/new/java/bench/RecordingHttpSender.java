package bench;

import org.openrewrite.ipc.http.HttpSender;

import java.util.concurrent.atomic.AtomicLong;

/** Wraps a real {@link HttpSender}, counting every request that flows through the resolver transport. */
public class RecordingHttpSender implements HttpSender {

    private final HttpSender delegate;
    private final AtomicLong count = new AtomicLong();

    public RecordingHttpSender(HttpSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response send(Request request) {
        count.incrementAndGet();
        return delegate.send(request);
    }

    public long count() {
        return count.get();
    }
}
