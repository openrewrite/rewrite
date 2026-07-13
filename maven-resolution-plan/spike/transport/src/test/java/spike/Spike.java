package spike;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.openrewrite.ipc.http.HttpSender;

import java.nio.file.Path;
import java.util.Collections;

/** Small shared helpers for the collect flow. */
final class Spike {

    private Spike() {
    }

    static CloseableSession session(RepositorySystem system, Path localRepo, HttpSender sender) {
        return new SessionBuilderSupplier(system).get()
                .withLocalRepositoryBaseDirectories(localRepo)
                // The sender is carried on the session, so it is resolved per session (not baked at bootstrap).
                .setConfigProperty(HttpSenderTransporterFactory.HTTP_SENDER_KEY, sender)
                .build();
    }

    static CollectResult collect(RepositorySystem system, RepositorySystemSession session, RemoteRepository repo,
                                 String gav) throws Exception {
        CollectRequest request = new CollectRequest();
        request.setRoot(new Dependency(new DefaultArtifact(gav), ""));
        request.setRepositories(Collections.singletonList(repo));
        return system.collectDependencies(session, request);
    }

    /** The single leaf child's artifactId at each level, asserting the linear app -> lib-a -> lib-b chain. */
    static String only(DependencyNode node) {
        return node.getArtifact().getArtifactId();
    }
}
