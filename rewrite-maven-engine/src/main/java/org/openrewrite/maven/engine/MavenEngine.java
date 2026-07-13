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

import lombok.Getter;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * Bootstrap facade: the only class that knows Maven Resolver is underneath. Owns a single plain-{@code new}
 * {@link RepositorySystem} (no DI container) and a shared {@link RepositoryCache}, and hands out sessions whose
 * template mirrors Maven 3.9's {@code DefaultRepositorySystemSessionFactory} — the parity reference for every knob.
 * <p>
 * One engine per resolution run, cached on the {@code ExecutionContext} by the host; one session per collect.
 */
public class MavenEngine implements Closeable {

    @Getter
    private final RepositorySystem repositorySystem;

    @Getter
    private final EngineOptions options;

    // Resolver's named-lock selection: use the in-JVM ReentrantReadWriteLock factory, never the default file locks.
    static final String NAMED_LOCK_FACTORY_KEY = "aether.syncContext.named.factory";
    static final String NAMED_LOCK_FACTORY_LOCAL = "rwlock-local";

    // Shared across every session this engine serves (descriptor/model results), like the factory's request cache.
    private final RepositoryCache repositoryCache = new DefaultRepositoryCache();

    public MavenEngine(EngineOptions options) {
        this.options = options;
        this.repositorySystem = new EngineRepositorySystemSupplier().get();
    }

    public MavenEngine() {
        this(EngineOptions.DEFAULT);
    }

    /**
     * Build a session against a private per-run scratch local repository. The template mirrors Maven 3.9's session
     * factory; the collection knobs (ClassicDependencyManager, verbose ConflictResolver chain, artifact-type registry)
     * are inherited unchanged from {@link SessionBuilderSupplier}, which itself reproduces
     * {@code MavenRepositorySystemUtils.newSession()}.
     *
     * @param localRepositoryScratch a private per-run directory; the engine never writes to {@code ~/.m2}
     * @param config                 the per-session sender, dead-endpoint set, and accounting hook
     */
    public CloseableSession newSession(Path localRepositoryScratch, SessionConfig config) {
        SessionBuilder builder = new SessionBuilderSupplier(repositorySystem).get()
                // factory L160: session.setCache(request.getRepositoryCache()) — one cache shared across sessions.
                .setCache(repositoryCache)
                // factory L163: ConfigurationProperties.USER_AGENT.
                .setConfigProperty(ConfigurationProperties.USER_AGENT, userAgent())
                // DESIGN §0/§5.3: private per-run scratch LRM; never ~/.m2.
                .withLocalRepositoryBaseDirectories(localRepositoryScratch)
                // factory L173 (value per DESIGN §5.3): rewrite never validated checksums; enabling later is announced.
                .setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE)
                // factory L179: neither -nsu nor -U set → null, defer to each repository's own policy.
                .setUpdatePolicy(null)
                // factory L182-190 with default request flags (cacheNotFound=false, cacheTransferError=false):
                // artifact policy CACHE_DISABLED(0), metadata policy CACHE_NOT_FOUND. rewrite's negative caching is
                // owned by MavenPomCache (a later slice), not aether's error policy.
                .setResolutionErrorPolicy(
                        new SimpleResolutionErrorPolicy(0, ResolutionErrorPolicy.CACHE_NOT_FOUND))
                // factory L353 (default isIgnoreTransitiveRepositories()=false): descriptor-declared repositories are
                // aggregated into child requests (DESIGN §4.2 alignment).
                .setIgnoreArtifactDescriptorRepositories(false)
                // newSession() L124: tolerate missing/invalid transitive descriptors, matching mvn dependency:tree.
                .setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true))
                // In-JVM named locks (ReentrantReadWriteLock) instead of the default file-lock factory: the private
                // per-run scratch LRM has no cross-process contention, so file locks are pure overhead — and each holds
                // an open FileChannel on a .locks/ file, which a BOM-heavy reactor's concurrent collect accumulates to
                // the OS file-descriptor cap. rwlock-local synchronizes the collector's worker threads with zero files.
                .setConfigProperty(NAMED_LOCK_FACTORY_KEY, NAMED_LOCK_FACTORY_LOCAL)
                // Per-session inputs the transport resolves on each newInstance.
                .setConfigProperty(HttpSenderTransporterFactory.HTTP_SENDER_KEY, config.getHttpSender())
                .setConfigProperty(HttpSenderTransporterFactory.UNREACHABLE_HOSTS_KEY, config.getUnreachableHosts())
                .setConfigProperty(HttpSenderTransporterFactory.RESOLUTION_TIME_RECORDER_KEY,
                        config.getResolutionTimeRecorder());
        return builder.build();
    }

    private static String userAgent() {
        return "OpenRewrite-Maven-Engine (Java " + System.getProperty("java.version") + "; " +
                System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }

    @Override
    public void close() {
        repositorySystem.shutdown();
    }
}
