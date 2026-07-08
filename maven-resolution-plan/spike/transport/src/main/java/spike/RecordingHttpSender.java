package spike;

import org.openrewrite.ipc.http.HttpSender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Wraps a real {@link HttpSender}, recording (method, path) of every request that flows through it. */
public class RecordingHttpSender implements HttpSender {

    public static final class Recorded {
        public final Method method;
        public final String path;

        Recorded(Method method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    private final HttpSender delegate;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    public RecordingHttpSender(HttpSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response send(Request request) {
        requests.add(new Recorded(request.getMethod(), request.getUrl().getPath()));
        return delegate.send(request);
    }

    public List<Recorded> requests() {
        return requests;
    }

    public long count() {
        return requests.size();
    }

    public long countPomGets() {
        return requests.stream()
                .filter(r -> r.method == Method.GET && r.path.endsWith(".pom"))
                .count();
    }

    public boolean anyJarRequested() {
        return requests.stream().anyMatch(r -> r.path.endsWith(".jar"));
    }
}
