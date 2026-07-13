package bench;

import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;

import java.util.HashMap;
import java.util.Map;

/** Stock no-DI supplier, customized to route http(s) through our {@link HttpSenderTransporterFactory} (crib spike/transport). */
class BenchRepositorySystemSupplier extends RepositorySystemSupplier {

    @Override
    protected Map<String, TransporterFactory> createTransporterFactories() {
        Map<String, TransporterFactory> factories = new HashMap<>();
        factories.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        factories.put("openrewrite-http", new HttpSenderTransporterFactory());
        return factories;
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        // Disable the stock GroupId/Prefixes filter sources; the Prefixes source reaches out to live Central.
        return new HashMap<>();
    }
}
