package bench;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.openrewrite.ipc.http.HttpSender;

/** Registers {@link HttpSenderTransporter} as the only http(s) transport; the sender is per-session (config property). */
public class HttpSenderTransporterFactory implements TransporterFactory {

    public static final String HTTP_SENDER_KEY = "openrewrite.httpSender";

    @Override
    public Transporter newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoTransporterException {
        String protocol = repository.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new NoTransporterException(repository, "only http/https handled by HttpSenderTransporter");
        }
        Object sender = session.getConfigProperties().get(HTTP_SENDER_KEY);
        if (!(sender instanceof HttpSender)) {
            throw new NoTransporterException(repository,
                    "no HttpSender in session config property '" + HTTP_SENDER_KEY + "'");
        }
        return new HttpSenderTransporter((HttpSender) sender, repository.getUrl());
    }

    @Override
    public float getPriority() {
        return 100.0f;
    }
}
