package bench;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * ModelResolver modelled on maven-core's {@code ProjectModelResolver}: reactor modules are served from an on-disk
 * GAV -> pom map (the WorkspaceModelResolver role); everything else (external parents / import BOMs) is resolved from
 * the configured remote repositories via the resolver {@link RepositorySystem}. Fixed versions only (no ranges), which
 * is all the maven-3.9.16 corpus needs, so parent/dependency resolution skips version-range lookups.
 */
class ReactorModelResolver implements ModelResolver {

    private final Map<String, File> reactor;
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    ReactorModelResolver(Map<String, File> reactor, RepositorySystem system, RepositorySystemSession session,
                         List<RemoteRepository> repositories) {
        this.reactor = reactor;
        this.system = system;
        this.session = session;
        this.repositories = repositories;
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        File local = reactor.get(groupId + ":" + artifactId + ":" + version);
        if (local != null) {
            return new FileModelSource(local);
        }
        Artifact pom = new DefaultArtifact(groupId, artifactId, "", "pom", version);
        try {
            pom = system.resolveArtifact(session, new ArtifactRequest(pom, repositories, "project")).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }
        return new FileModelSource(pom.getFile());
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}
