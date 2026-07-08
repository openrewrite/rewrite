package spike;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claim 3: the {@code ArtifactDescriptorReader} can be decorated via the supplier's override point with a GAV-keyed
 * cache that short-circuits descriptor reads entirely — zero network even against a fresh, empty local repository.
 * This is the seam rewrite-maven's pluggable {@code MavenPomCache} would occupy.
 */
class T3_DescriptorDecorationTest {

    @Test
    void cachedDescriptorReaderServesSecondSessionWithZeroNetwork(@TempDir Path localA, @TempDir Path localB)
            throws Exception {
        SpikeRepositorySystemSupplier supplier = new SpikeRepositorySystemSupplier(true);
        try (RepositorySystem system = supplier.get();
             TinyMavenRepo repo = new TinyMavenRepo()) {

            HttpUrlConnectionSender sender = new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));

            // First session with a fresh local repo: populates the decorator cache, hits the network.
            try (CloseableSession sessionA = Spike.session(system, localA, sender)) {
                assertGraph(Spike.collect(system, sessionA, repo.remoteRepository(), "com.example:app:1"));
            }
            int requestsAfterFirst = repo.serverRequestCount();
            assertTrue(requestsAfterFirst > 0, "first session must hit the network");
            assertEquals(3, supplier.cachingReader().misses(), "3 cold descriptor reads (app, lib-a, lib-b)");

            // Second session with a DIFFERENT, empty local repo but the same decorator/cache.
            try (CloseableSession sessionB = Spike.session(system, localB, sender)) {
                assertGraph(Spike.collect(system, sessionB, repo.remoteRepository(), "com.example:app:1"));
            }

            assertEquals(requestsAfterFirst, repo.serverRequestCount(),
                    "second session must make ZERO new requests (descriptor cache short-circuited every read)");
            assertTrue(supplier.cachingReader().hits() >= 3,
                    "second session served entirely from cache: " + supplier.cachingReader().hits() + " hits");
        }
    }

    private static void assertGraph(CollectResult result) {
        DependencyNode app = result.getRoot();
        assertEquals("app", app.getArtifact().getArtifactId());
        DependencyNode libA = app.getChildren().get(0);
        assertEquals("lib-a", libA.getArtifact().getArtifactId());
        DependencyNode libB = libA.getChildren().get(0);
        assertEquals("lib-b", libB.getArtifact().getArtifactId());
    }
}
