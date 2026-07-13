package spike;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claim 2: all remote traffic routes through an injected {@link HttpSender}, resolved PER SESSION (not baked at
 * bootstrap). Part A proves per-session routing and no bypass; Part B proves peek->HEAD and the load-bearing
 * {@link Transporter#classify} 404-vs-500 distinction.
 */
class T2_HttpInjectionTest {

    @Test
    void trafficRoutesThroughPerSessionSender_noBypass(@TempDir Path localA, @TempDir Path localB) throws Exception {
        // One bootstrap, two sessions, two distinct senders.
        try (RepositorySystem system = new SpikeRepositorySystemSupplier(false).get();
             TinyMavenRepo repo = new TinyMavenRepo()) {

            RecordingHttpSender senderA =
                    new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10)));
            RecordingHttpSender senderB =
                    new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10)));

            try (CloseableSession sessionA = Spike.session(system, localA, senderA)) {
                Spike.collect(system, sessionA, repo.remoteRepository(), "com.example:app:1");
            }
            try (CloseableSession sessionB = Spike.session(system, localB, senderB)) {
                Spike.collect(system, sessionB, repo.remoteRepository(), "com.example:app:1");
            }

            // Each session's traffic landed on its own sender (3 POM GETs each: app, lib-a, lib-b).
            assertEquals(3, senderA.countPomGets(), "session A POM GETs: " + senderA.requests());
            assertEquals(3, senderB.countPomGets(), "session B POM GETs: " + senderB.requests());
            assertFalse(senderA.anyJarRequested());
            assertFalse(senderB.anyJarRequested());

            // No bypass: every request the server saw came through one of the injected senders.
            assertEquals(senderA.count() + senderB.count(), repo.serverRequestCount(),
                    "server request count must equal the sum of both recorders (no request bypassed the sender)"
                            + "\n  A=" + senderA.requests() + "\n  B=" + senderB.requests()
                            + "\n  server=" + repo.requested);
        }
    }

    @Test
    void peekMapsToHead_andClassifyDistinguishes404From500() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    boolean head = "HEAD".equals(request.getMethod());
                    switch (request.getPath()) {
                        case "/exists.pom":
                            MockResponse ok = new MockResponse().setResponseCode(200);
                            // Never send a body on a HEAD, or its bytes poison the next keep-alive request.
                            return head ? ok.setHeader("Content-Length", "10") : ok.setBody("<project/>");
                        case "/missing.pom":
                            return new MockResponse().setResponseCode(404);
                        case "/broken.pom":
                            return new MockResponse().setResponseCode(500);
                        default:
                            return new MockResponse().setResponseCode(404);
                    }
                }
            });
            server.start();

            RecordingHttpSender sender =
                    new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)));
            Transporter transporter = new HttpSenderTransporter(sender, server.url("/").toString());

            // peek() on an existing resource issues a HEAD and succeeds.
            transporter.peek(new PeekTask(URI.create("exists.pom")));
            RecordingHttpSender.Recorded lastPeek = sender.requests().get(sender.requests().size() - 1);
            assertEquals(HttpSender.Method.HEAD, lastPeek.method, "PEEK must map to HEAD");

            // 404 -> ERROR_NOT_FOUND (feeds resolver negative caching).
            Exception notFound = assertThrows(Exception.class,
                    () -> transporter.peek(new PeekTask(URI.create("missing.pom"))));
            assertEquals(Transporter.ERROR_NOT_FOUND, transporter.classify(notFound),
                    "peek 404 threw " + notFound.getClass().getName() + ": " + notFound.getMessage());

            Path tmp = Files.createTempFile("get", ".pom");
            Exception getNotFound = assertThrows(Exception.class,
                    () -> transporter.get(new GetTask(URI.create("missing.pom")).setDataPath(tmp)));
            assertEquals(Transporter.ERROR_NOT_FOUND, transporter.classify(getNotFound),
                    "get 404 threw " + getNotFound.getClass().getName() + ": " + getNotFound.getMessage());

            // 500 -> ERROR_OTHER (transfer error, not "not found").
            Exception serverError = assertThrows(Exception.class,
                    () -> transporter.get(new GetTask(URI.create("broken.pom")).setDataPath(tmp)));
            assertEquals(Transporter.ERROR_OTHER, transporter.classify(serverError),
                    "get 500 threw " + serverError.getClass().getName() + ": " + serverError.getMessage());

            // A successful GET downloads the body through the sender.
            Path out = Files.createTempFile("ok", ".pom");
            transporter.get(new GetTask(URI.create("exists.pom")).setDataPath(out));
            assertEquals("<project/>", new String(Files.readAllBytes(out)));

            transporter.close();
        }
    }

    /**
     * Ground truth: despite forcing {@code setDoOutput(true)} on every non-GET method, rewrite's
     * {@link HttpUrlConnectionSender} still issues a real HEAD on the wire, so it is fit to back transporter peek.
     */
    @Test
    void httpUrlConnectionSenderIssuesRealHeadOnTheWire() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Length", "0"));
            server.start();
            HttpSender huc = new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5));
            huc.send(huc.head(server.url("/probe").toString()).build()).close();
            assertEquals("HEAD", server.takeRequest().getMethod());
        }
    }
}
