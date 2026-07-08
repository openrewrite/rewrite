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
package org.openrewrite.maven.internal.engine;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.FileModelSource;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelSource;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.UnresolvableModelException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystem;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactResolutionException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactResult;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionRangeRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionRangeResult;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.version.Version;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The pom-XML supply chain the model builder resolves parents and imported BOMs through. Given a GAV, it consults the
 * pom-bytes region of {@link MavenPomCache} (tri-state: a known-absent entry replays the not-found with no I/O), and on
 * a miss resolves the {@code .pom} through the engine's {@link RepositorySystem} (the {@code ArtifactResolver} path over
 * the private per-run scratch LRM, {@code HttpSenderTransporter} doing the byte transfer and 4xx/transfer
 * classification). Success populates the bytes region and, parse-through, the parsed-{@link Pom} region; the repository
 * that served each GAV is recorded so B2 can attribute {@code gav → repository}.
 * <p>
 * Repository iteration mirrors {@code MavenPomDownloader.download}: each repository is keyed independently
 * ({@code ResolvedGroupArtifactVersion(repo.uri, g, a, v)}), so a warm hit or a cached negative is per-repository, and a
 * deterministic 4xx caches a negative while a transfer error never does.
 */
public class CacheBridge {

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final MavenPomCache pomCache;
    private final Path materializeDir;

    private final Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy = new ConcurrentHashMap<>();
    private final Map<GroupArtifactVersion, Map<MavenRepository, String>> responsesByGav = new ConcurrentHashMap<>();

    public CacheBridge(RepositorySystem system, RepositorySystemSession session, MavenPomCache pomCache,
                       Path materializeDir) {
        this.system = system;
        this.session = session;
        this.pomCache = pomCache;
        this.materializeDir = materializeDir;
    }

    /** {@code gav → repository} that served it this run; empty for GAVs answered from the warm bytes region (no repo re-derived). */
    public Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy() {
        return servedBy;
    }

    /** The per-repository responses recorded for a GAV that failed to resolve, for {@link ModelParityErrorMapper}. */
    public @Nullable Map<MavenRepository, String> responsesFor(GroupArtifactVersion gav) {
        return responsesByGav.get(gav);
    }

    RepositorySystemSession session() {
        return session;
    }

    /**
     * Resolve a pom to a {@link ModelSource}, walking {@code repositories} in order. A bytes-region hit is materialized
     * to a real file (the resolver requires a {@link Path}); a known-absent entry short-circuits that repository with no
     * I/O; a miss is resolved through the engine and cached (positive bytes + parse-through, or a 4xx negative).
     */
    public ModelSource resolvePom(String groupId, String artifactId, String version, List<MavenRepository> repositories)
            throws UnresolvableModelException {
        Map<MavenRepository, String> responses = new LinkedHashMap<>();
        for (MavenRepository repo : repositories) {
            ResolvedGroupArtifactVersion key =
                    new ResolvedGroupArtifactVersion(repo.getUri(), groupId, artifactId, version, null);

            Optional<byte[]> cached = readBytes(key);
            if (cached != null) {
                if (cached.isPresent()) {
                    servedBy.put(key, repo);
                    return new FileModelSource(materialize(groupId, artifactId, version, cached.get()));
                }
                responses.put(repo, "not found (cached)");
                continue;
            }

            try {
                ArtifactResult result = resolveArtifact(repo, groupId, artifactId, version);
                File pomFile = result.getArtifact().getFile();
                byte[] bytes = Files.readAllBytes(pomFile.toPath());
                pomCache.putPomBytes(key, bytes);
                parseThrough(key, repo, bytes);
                servedBy.put(key, repo);
                return new FileModelSource(pomFile);
            } catch (ArtifactResolutionException e) {
                if (isNotFound(e)) {
                    pomCache.putPomBytes(key, null); // deterministic 4xx → cache the negative
                    responses.put(repo, "not found");
                } else {
                    responses.put(repo, rootMessage(e)); // transfer error → never cached
                }
            } catch (IOException e) {
                responses.put(repo, rootMessage(e));
            }
        }
        responsesByGav.put(new GroupArtifactVersion(groupId, artifactId, version), responses);
        throw new UnresolvableModelException(
                "Could not resolve " + groupId + ":" + artifactId + ":" + version + " " + responses,
                groupId, artifactId, version);
    }

    /**
     * Resolve a {@code <parent>}/BOM version that may be a range (e.g. {@code [1.0,2.0)}) to the highest matching
     * version via the engine's {@code VersionRangeResolver}; a plain version is returned unchanged with no I/O.
     */
    public String resolveHighestMatchingVersion(String groupId, String artifactId, String version,
                                                List<MavenRepository> repositories) throws UnresolvableModelException {
        if (!isRange(version)) {
            return version;
        }
        VersionRangeRequest request = new VersionRangeRequest(
                new DefaultArtifact(groupId, artifactId, "", "pom", version), toRemote(repositories), "");
        try {
            VersionRangeResult result = system.resolveVersionRange(session, request);
            Version highest = result.getHighestVersion();
            if (highest == null) {
                throw new UnresolvableModelException(
                        "No versions available for " + groupId + ":" + artifactId + ":" + version,
                        groupId, artifactId, version);
            }
            return highest.toString();
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(e, groupId, artifactId, version);
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    private @Nullable Optional<byte[]> readBytes(ResolvedGroupArtifactVersion key) {
        try {
            return pomCache.getPomBytes(key);
        } catch (MavenDownloadingException e) {
            return null; // a cache read failure is treated as unknown; the engine resolves it fresh
        }
    }

    private void parseThrough(ResolvedGroupArtifactVersion key, MavenRepository repo, byte[] bytes) {
        Path inputPath = Paths.get(key.getGroupId(), key.getArtifactId(), key.getVersion());
        RawPom raw = RawPom.parse(new ByteArrayInputStream(bytes), null);
        pomCache.putPom(key, raw.toPom(inputPath, repo).withGav(key));
    }

    private File materialize(String groupId, String artifactId, String version, byte[] bytes) {
        Path file = materializeDir
                .resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version)
                .resolve(artifactId + "-" + version + ".pom");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.write(file, bytes);
            }
        } catch (IOException e) {
            throw new UncheckedMaterializationException(file, e);
        }
        return file.toFile();
    }

    private ArtifactResult resolveArtifact(MavenRepository repo, String groupId, String artifactId, String version)
            throws ArtifactResolutionException {
        ArtifactRequest request = new ArtifactRequest(
                new DefaultArtifact(groupId, artifactId, "", "pom", version),
                Collections.singletonList(toRemote(repo)), "");
        return system.resolveArtifact(session, request);
    }

    private List<RemoteRepository> toRemote(List<MavenRepository> repositories) {
        List<RemoteRepository> remotes = new ArrayList<>(repositories.size());
        for (MavenRepository repo : repositories) {
            remotes.add(toRemote(repo));
        }
        return remotes;
    }

    private RemoteRepository toRemote(MavenRepository repo) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(
                repo.getId() == null ? repo.getUri() : repo.getId(), "default", repo.getUri())
                .setReleasePolicy(policy(repo.getReleases()))
                .setSnapshotPolicy(policy(repo.getSnapshots()));
        if (repo.getUsername() != null && repo.getPassword() != null) {
            builder.setAuthentication(new AuthenticationBuilder()
                    .addUsername(repo.getUsername())
                    .addPassword(repo.getPassword())
                    .build());
        }
        return builder.build();
    }

    // CHECKSUM_POLICY_IGNORE mirrors the session (rewrite never validated); ALWAYS because the scratch LRM is empty.
    private static RepositoryPolicy policy(@Nullable String enabled) {
        return new RepositoryPolicy(!"false".equalsIgnoreCase(enabled),
                RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
    }

    private static boolean isNotFound(ArtifactResolutionException e) {
        if (e.getResult() != null) {
            for (Exception ex : e.getResult().getExceptions()) {
                for (Throwable t = ex; t != null; t = t.getCause()) {
                    if (t instanceof ArtifactNotFoundException) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isRange(String version) {
        return version.startsWith("[") || version.startsWith("(");
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private static final class UncheckedMaterializationException extends RuntimeException {
        UncheckedMaterializationException(Path file, IOException cause) {
            super("Could not materialize cached pom bytes to " + file, cause);
        }
    }
}
