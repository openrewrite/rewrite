/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.engine;

import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.util.ConfigUtils;
import org.openrewrite.ipc.http.HttpSender;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registers {@link HttpSenderTransporter} as the only http(s) transport. Every per-session input — the sender, the
 * dead-endpoint set, the accounting hook — is resolved from the session on each {@link #newInstance}, so one
 * bootstrapped {@code RepositorySystem} can serve many sessions, each tunneling through a different sender. Per-server
 * credentials, headers, and timeouts come from the session exactly as Maven's {@code DefaultRepositorySystemSessionFactory}
 * carries them (auth selector, {@code aether.transport.http.*} config properties).
 */
public class HttpSenderTransporterFactory implements TransporterFactory {

    /** Session config-property key under which the per-session {@link HttpSender} instance is stored. */
    public static final String HTTP_SENDER_KEY = "openrewrite.httpSender";

    /** Session config-property key holding the run-scoped {@code Set<String>} of unreachable {@code host:port}. */
    public static final String UNREACHABLE_HOSTS_KEY = "openrewrite.unreachableHosts";

    /** Session config-property key holding the {@link ResolutionTimeRecorder}. */
    public static final String RESOLUTION_TIME_RECORDER_KEY = "openrewrite.resolutionTimeRecorder";

    // MavenPomDownloader's defaults: connect 10s, read 30s.
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 30_000;

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

        String repoId = repository.getId();
        String username = null;
        String password = null;
        AuthenticationContext authCtx = AuthenticationContext.forRepository(session, repository);
        if (authCtx != null) {
            try {
                username = authCtx.get(AuthenticationContext.USERNAME);
                password = authCtx.get(AuthenticationContext.PASSWORD);
            } finally {
                authCtx.close();
            }
        }

        return new HttpSenderTransporter(
                (HttpSender) sender,
                repository.getUrl(),
                username,
                password,
                headers(session, repoId),
                Duration.ofMillis(ConfigUtils.getInteger(session, DEFAULT_CONNECT_TIMEOUT_MS,
                        ConfigurationProperties.CONNECT_TIMEOUT + "." + repoId, ConfigurationProperties.CONNECT_TIMEOUT)),
                Duration.ofMillis(ConfigUtils.getInteger(session, DEFAULT_READ_TIMEOUT_MS,
                        ConfigurationProperties.REQUEST_TIMEOUT + "." + repoId, ConfigurationProperties.REQUEST_TIMEOUT)),
                unreachableHosts(session),
                resolutionTimeRecorder(session));
    }

    private static Map<String, String> headers(RepositorySystemSession session, String repoId) {
        Map<?, ?> configured = ConfigUtils.getMap(session, Collections.emptyMap(),
                ConfigurationProperties.HTTP_HEADERS + "." + repoId, ConfigurationProperties.HTTP_HEADERS);
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : configured.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                headers.put(e.getKey().toString(), e.getValue().toString());
            }
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> unreachableHosts(RepositorySystemSession session) {
        Object value = session.getConfigProperties().get(UNREACHABLE_HOSTS_KEY);
        return value instanceof Set ? (Set<String>) value : Collections.emptySet();
    }

    private static ResolutionTimeRecorder resolutionTimeRecorder(RepositorySystemSession session) {
        Object value = session.getConfigProperties().get(RESOLUTION_TIME_RECORDER_KEY);
        return value instanceof ResolutionTimeRecorder ? (ResolutionTimeRecorder) value : ResolutionTimeRecorder.NOOP;
    }

    @Override
    public float getPriority() {
        // Beat the stock ApacheTransporterFactory (priority 5.0f) so this is chosen for http/https.
        return 100.0f;
    }
}
