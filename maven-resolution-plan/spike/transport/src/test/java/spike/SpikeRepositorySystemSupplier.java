package spike;

import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The whole point of the spike: the stock no-DI supplier customized purely through its {@code protected create*()}
 * override points — no container, no reflection.
 * <ul>
 *   <li>{@link #createTransporterFactories()} replaces the stock Apache http transport with our
 *       {@link HttpSenderTransporterFactory} (claim 2), keeping file transport.</li>
 *   <li>{@link #createArtifactDescriptorReader()} optionally wraps the POM reader in a cache (claim 3).</li>
 * </ul>
 */
class SpikeRepositorySystemSupplier extends RepositorySystemSupplier {

    private final boolean cacheDescriptors;
    private CachingArtifactDescriptorReader cachingReader;

    SpikeRepositorySystemSupplier(boolean cacheDescriptors) {
        this.cacheDescriptors = cacheDescriptors;
    }

    @Override
    protected Map<String, TransporterFactory> createTransporterFactories() {
        Map<String, TransporterFactory> factories = new HashMap<>();
        factories.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        // Note the deliberate absence of ApacheTransporterFactory: this is now the ONLY http/https transport.
        factories.put("openrewrite-http", new HttpSenderTransporterFactory());
        return factories;
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        // GOTCHA: the stock mvn3 supplier registers GroupId + Prefixes filter sources with DEFAULT_ENABLED=true.
        // When active, the Prefixes source GETs "<repo>/.meta/prefixes.txt" from every remote repo AND (observed)
        // reaches out to live Maven Central — a hermeticity landmine. Disable them for deterministic resolution.
        return new HashMap<>();
    }

    @Override
    protected ArtifactDescriptorReader createArtifactDescriptorReader() {
        ArtifactDescriptorReader delegate = super.createArtifactDescriptorReader();
        if (cacheDescriptors) {
            cachingReader = new CachingArtifactDescriptorReader(delegate);
            return cachingReader;
        }
        return delegate;
    }

    CachingArtifactDescriptorReader cachingReader() {
        return cachingReader;
    }
}
