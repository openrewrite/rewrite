package bench;

import org.eclipse.aether.spi.connector.transport.AbstractTransporter;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Resolver transporter that pumps every byte through an injected OpenRewrite {@link HttpSender} (cribbed from spike/transport). */
class HttpSenderTransporter extends AbstractTransporter {

    private final HttpSender httpSender;
    private final URI baseUri;

    HttpSenderTransporter(HttpSender httpSender, String repositoryUrl) {
        this.httpSender = httpSender;
        String url = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        this.baseUri = URI.create(url);
    }

    @Override
    public int classify(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ResourceNotFoundException) {
                return ERROR_NOT_FOUND;
            }
        }
        return ERROR_OTHER;
    }

    @Override
    protected void implPeek(PeekTask task) throws Exception {
        try (HttpSender.Response response = httpSender.send(httpSender.head(resolve(task.getLocation())).build())) {
            failIfError(response);
        }
    }

    @Override
    protected void implGet(GetTask task) throws Exception {
        HttpSender.Response response = httpSender.send(httpSender.get(resolve(task.getLocation())).build());
        try {
            failIfError(response);
            long length = contentLength(response.getHeaders());
            try (InputStream body = response.getBody()) {
                utilGet(task, body, true, length, false);
            }
        } finally {
            response.close();
        }
    }

    @Override
    protected void implPut(PutTask task) throws Exception {
        try (InputStream content = task.newInputStream()) {
            byte[] bytes = readAll(content);
            HttpSender.Request request = httpSender.put(resolve(task.getLocation()))
                    .withContent("application/octet-stream", bytes)
                    .build();
            try (HttpSender.Response response = httpSender.send(request)) {
                failIfError(response);
            }
        }
    }

    @Override
    protected void implClose() {
    }

    private String resolve(URI location) {
        return baseUri.resolve(location).toString();
    }

    private static void failIfError(HttpSender.Response response) throws Exception {
        int code = response.getCode();
        if (code == 404 || code == 410) {
            throw new ResourceNotFoundException(code);
        }
        if (code >= 400) {
            throw new HttpTransferException(code);
        }
    }

    private static long contentLength(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if ("Content-Length".equalsIgnoreCase(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty()) {
                try {
                    return Long.parseLong(e.getValue().get(0));
                } catch (NumberFormatException ignored) {
                    return -1L;
                }
            }
        }
        return -1L;
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n = is.read(buf); n >= 0; n = is.read(buf)) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    static final class ResourceNotFoundException extends Exception {
        ResourceNotFoundException(int code) {
            super("HTTP " + code + " (not found)");
        }
    }

    static final class HttpTransferException extends Exception {
        HttpTransferException(int code) {
            super("HTTP " + code + " (transfer error)");
        }
    }
}
