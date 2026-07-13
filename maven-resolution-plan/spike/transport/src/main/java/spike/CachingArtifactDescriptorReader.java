package spike;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decorates the stock {@link ArtifactDescriptorReader} (which does the POM download + model build) with a simple
 * GAV-keyed in-memory cache. A cache hit short-circuits the read entirely — zero network, zero local-repo access —
 * which is exactly the seam rewrite-maven's pluggable {@code MavenPomCache} would occupy. The cache instance is shared
 * across every session served by the owning {@code RepositorySystem}.
 */
public class CachingArtifactDescriptorReader implements ArtifactDescriptorReader {

    private final ArtifactDescriptorReader delegate;
    private final Map<String, ArtifactDescriptorResult> cache = new ConcurrentHashMap<>();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger misses = new AtomicInteger();

    public CachingArtifactDescriptorReader(ArtifactDescriptorReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(
            RepositorySystemSession session, ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        String key = key(request.getArtifact());
        ArtifactDescriptorResult cached = cache.get(key);
        if (cached != null) {
            hits.incrementAndGet();
            return copyFor(request, cached);
        }
        misses.incrementAndGet();
        ArtifactDescriptorResult result = delegate.readArtifactDescriptor(session, request);
        cache.put(key, result);
        return result;
    }

    /** Re-home a cached result onto the current request so the collector sees request-scoped repositories. */
    private static ArtifactDescriptorResult copyFor(ArtifactDescriptorRequest request, ArtifactDescriptorResult src) {
        ArtifactDescriptorResult copy = new ArtifactDescriptorResult(request);
        copy.setArtifact(src.getArtifact());
        copy.setRepository(src.getRepository());
        copy.setDependencies(src.getDependencies());
        copy.setManagedDependencies(src.getManagedDependencies());
        copy.setRepositories(src.getRepositories());
        copy.setRelocations(src.getRelocations());
        copy.setAliases(src.getAliases());
        copy.setProperties(src.getProperties());
        return copy;
    }

    private static String key(Artifact a) {
        return a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getClassifier() + ":" + a.getExtension() + ":"
                + a.getVersion();
    }

    public int hits() {
        return hits.get();
    }

    public int misses() {
        return misses.get();
    }
}
