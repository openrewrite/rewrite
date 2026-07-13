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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claim 1: {@code maven-resolver-supplier-mvn3} bootstraps a working RepositorySystem with plain {@code new}
 * (no DI container) and collects a dependency graph.
 */
class T1_BootstrapCollectTest {

    @Test
    void bootstrapWithPlainNewAndCollectGraph(@TempDir Path localRepo) throws Exception {
        // Plain `new` — no Guice/Sisu/Plexus container anywhere.
        try (RepositorySystem system = new SpikeRepositorySystemSupplier(false).get();
             TinyMavenRepo repo = new TinyMavenRepo()) {

            HttpUrlConnectionSender sender = new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));

            try (CloseableSession session = Spike.session(system, localRepo, sender)) {
                CollectResult result = Spike.collect(system, session, repo.remoteRepository(), "com.example:app:1");

                DependencyNode app = result.getRoot();
                assertEquals("app", app.getArtifact().getArtifactId());

                List<DependencyNode> appChildren = app.getChildren();
                assertEquals(1, appChildren.size(), "app should have exactly one dependency");
                DependencyNode libA = appChildren.get(0);
                assertEquals("lib-a", libA.getArtifact().getArtifactId());

                List<DependencyNode> libAChildren = libA.getChildren();
                assertEquals(1, libAChildren.size(), "lib-a should have exactly one dependency");
                DependencyNode libB = libAChildren.get(0);
                assertEquals("lib-b", libB.getArtifact().getArtifactId());
                assertTrue(libB.getChildren().isEmpty(), "lib-b is a leaf");
            }

            // Collection reads POMs only, never JARs.
            assertFalse(repo.anyJarRequested(), "collect must not request any .jar: " + repo.requested);
            assertEquals(3, repo.pomRequestCount(), "exactly the app, lib-a, lib-b POMs: " + repo.requested);
        }
    }
}
