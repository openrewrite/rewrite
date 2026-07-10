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
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.HttpSenderTransporterFactory;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.MetadataResolver;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.metadata.Metadata;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.MetadataRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.MetadataResult;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.transfer.MetadataNotFoundException;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorates the engine's {@code DefaultMetadataResolver}, routing every non-pinned metadata read/write during collection
 * through {@link MavenPomCache}'s metadata region — the same tri-state, deterministic-4xx-negative flow the pom bytes
 * take through {@link CacheBridge}. A region hit reconstructs the {@code maven-metadata.xml} the version resolver reads
 * (it consumes {@code metadata.getFile()}, verified in {@code DefaultVersionResolver.readVersions}) into the scratch dir
 * with zero network; a known-absent entry replays a {@link MetadataNotFoundException}; a miss delegates, then parses the
 * resolver's own {@code maven-metadata.xml} into a {@link MavenMetadata} and populates the region (a not-found first
 * attempts HTML-index derivation, see {@link #deriveIfMissing}, then caches a negative; a transfer error caches
 * nothing). Snapshot pins are unaffected: {@link PinnedVersionResolver} short-circuits them before any metadata request
 * is issued.
 */
class RegionMetadataResolver implements MetadataResolver {

    private static final DateTimeFormatter LAST_UPDATED =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final MetadataResolver delegate;

    // Repositories whose HTML-index derivation was denied (a non-404 deterministic 4xx on the scrape): disabled for
    // the rest of the run, mirroring legacy's repo.setDeriveMetadataIfMissing(false). Scrapes serialize per repository
    // so a concurrent collect observes the disable rather than racing past it.
    private final Set<String> derivationDisabled = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Object> derivationLocks = new ConcurrentHashMap<>();

    RegionMetadataResolver(MetadataResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session,
                                                Collection<? extends MetadataRequest> requests) {
        MavenPomCache cache = CollectContext.from(session).getPomCache();
        Path scratch = CollectContext.from(session).getMaterializeDir();

        List<MetadataResult> results = new ArrayList<>(requests.size());
        List<MetadataRequest> misses = new ArrayList<>();
        List<Integer> missSlots = new ArrayList<>();

        for (MetadataRequest request : requests) {
            URI repo = repoUri(request);
            if (repo == null) { // local-repository metadata: no region, delegate unchanged
                results.add(null);
                missSlots.add(results.size() - 1);
                misses.add(request);
                continue;
            }
            GroupArtifactVersion gav = keyFor(request.getMetadata());
            Optional<MavenMetadata> region = cache.getMavenMetadata(repo, gav);
            if (region == null) {
                results.add(null);
                missSlots.add(results.size() - 1);
                misses.add(request);
            } else if (region.isPresent()) {
                results.add(hit(request, region.get(), scratch));
            } else {
                results.add(absent(request));
            }
        }

        if (!misses.isEmpty()) {
            List<MetadataResult> delegated = delegate.resolveMetadata(session, misses);
            for (int i = 0; i < delegated.size(); i++) {
                MetadataResult r = delegated.get(i);
                MetadataResult derived = deriveIfMissing(session, cache, scratch, misses.get(i), r);
                if (derived != null) {
                    results.set(missSlots.get(i), derived);
                } else {
                    results.set(missSlots.get(i), r);
                    store(cache, misses.get(i), r);
                }
            }
        }
        return results;
    }

    /**
     * Legacy {@code MavenPomDownloader.deriveMetadata}: when {@code maven-metadata.xml} 404s and no explicit version
     * was requested, derive the version list from the repository's HTML directory index (Nexus-style). This only
     * produces an answer where Maven produces an error, never a different answer where Maven has one. The derived
     * result lands in the same metadata region, so it is never re-derived per read.
     */
    private @Nullable MetadataResult deriveIfMissing(RepositorySystemSession session, MavenPomCache cache,
                                                     Path scratch, MetadataRequest request, MetadataResult result) {
        Exception exception = result.getException();
        if (exception == null || !rootedIn(exception, MetadataNotFoundException.class)) {
            return null;
        }
        Metadata metadata = request.getMetadata();
        URI repoUri = repoUri(request);
        if (repoUri == null || metadata.getVersion() != null && !metadata.getVersion().isEmpty()) {
            return null;
        }
        String scheme = repoUri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return null;
        }
        String base = repoUri.toString().endsWith("/") ? repoUri.toString() : repoUri + "/";
        MavenRepository requestRepo = requestRepository(session, base);
        if (requestRepo != null && Boolean.FALSE.equals(requestRepo.getDeriveMetadataIfMissing())) {
            return null;
        }
        Object sender = session.getConfigProperties().get(HttpSenderTransporterFactory.HTTP_SENDER_KEY);
        if (!(sender instanceof HttpSender)) {
            return null;
        }
        String indexUrl = base + metadata.getGroupId().replace('.', '/') + '/' + metadata.getArtifactId() + '/';
        MavenMetadata.Versioning versioning;
        synchronized (derivationLocks.computeIfAbsent(base, k -> new Object())) {
            if (derivationDisabled.contains(base)) {
                return null;
            }
            versioning = scrape((HttpSender) sender, requestRepo, base, indexUrl);
        }
        if (versioning == null) {
            return null;
        }
        MavenMetadata derived = new MavenMetadata(versioning);
        cache.putMavenMetadata(repoUri, keyFor(metadata), derived);
        return hit(request, derived, scratch);
    }

    private MavenMetadata.@Nullable Versioning scrape(HttpSender sender, @Nullable MavenRepository repo,
                                                      String base, String indexUrl) {
        HttpSender.Request.Builder builder = sender.get(indexUrl);
        if (repo != null && repo.getUsername() != null && repo.getPassword() != null) {
            builder.withBasicAuthentication(repo.getUsername(), repo.getPassword());
        }
        try (HttpSender.Response response = sender.send(builder.build())) {
            int code = response.getCode();
            if (code >= 400) {
                if (code != 404 && isDeterministicClientError(code)) {
                    derivationDisabled.add(base);
                }
                return null;
            }
            InputStream body = response.getBody();
            return body == null ? null : htmlIndexToVersioning(new String(readAll(body), StandardCharsets.UTF_8), indexUrl);
        } catch (IOException | RuntimeException e) {
            return null; // a transfer failure never disables derivation
        }
    }

    private static boolean isDeterministicClientError(int code) {
        return code >= 400 && code <= 499 && code != 408 && code != 425 && code != 429;
    }

    private static @Nullable MavenRepository requestRepository(RepositorySystemSession session, String base) {
        for (MavenRepository repo : CollectContext.from(session).getRequestRepositories()) {
            String uri = repo.getUri().endsWith("/") ? repo.getUri() : repo.getUri() + "/";
            if (uri.equals(base)) {
                return repo;
            }
        }
        return null;
    }

    // Same primitive scrape as legacy: hrefs ending in "/" (excluding "../") are version directories.
    private static MavenMetadata.@Nullable Versioning htmlIndexToVersioning(String responseBody, String uri) {
        List<String> versions = new ArrayList<>();
        int start = responseBody.indexOf("<a href=\"");
        while (start > 0) {
            start += 9;
            int end = responseBody.indexOf("\"", start);
            if (end < 0) {
                break;
            }
            String href = responseBody.substring(start, end).trim();
            if (!href.startsWith("../") && href.endsWith("/")) {
                versions.add(hrefToVersion(href, uri));
            }
            start = responseBody.indexOf("<a href=\"", end);
        }
        return versions.isEmpty() ? null : new MavenMetadata.Versioning(versions, null, null, null, null, null);
    }

    private static String hrefToVersion(String href, String rootUri) {
        String version = href.startsWith(rootUri) ? href.substring(rootUri.length()) : href;
        return version.endsWith("/") ? version.substring(0, version.length() - 1) : version;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int read; (read = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private MetadataResult hit(MetadataRequest request, MavenMetadata md, Path scratch) {
        Metadata metadata = request.getMetadata();
        File file = materialize(scratch, request, toXml(metadata, md));
        return new MetadataResult(request).setMetadata(metadata.setFile(file)).setUpdated(true);
    }

    private static MetadataResult absent(MetadataRequest request) {
        return new MetadataResult(request).setException(
                new MetadataNotFoundException(request.getMetadata(), request.getRepository()));
    }

    private void store(MavenPomCache cache, MetadataRequest request, MetadataResult result) {
        URI repo = repoUri(request);
        if (repo == null) {
            return;
        }
        GroupArtifactVersion gav = keyFor(request.getMetadata());
        Exception exception = result.getException();
        if (exception != null) {
            if (rootedIn(exception, MetadataNotFoundException.class)) {
                cache.putMavenMetadata(repo, gav, null); // deterministic not-found → negative
            }
            return; // transfer error → cache nothing
        }
        File file = result.getMetadata() == null ? null : result.getMetadata().getFile();
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            MavenMetadata parsed = MavenMetadata.parse(Files.readAllBytes(file.toPath()));
            if (parsed != null) {
                cache.putMavenMetadata(repo, gav, parsed);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @Nullable URI repoUri(MetadataRequest request) {
        RemoteRepository repo = request.getRepository();
        return repo == null ? null : URI.create(repo.getUrl());
    }

    // Mirrors the legacy downloader's metadata key: G:A-level listings carry a null version, G:A:V snapshot metadata the
    // base version. Aether models the former as an empty version string.
    private static GroupArtifactVersion keyFor(Metadata metadata) {
        String version = metadata.getVersion();
        return new GroupArtifactVersion(metadata.getGroupId(), metadata.getArtifactId(),
                version == null || version.isEmpty() ? null : version);
    }

    private static File materialize(Path scratch, MetadataRequest request, byte[] bytes) {
        Metadata m = request.getMetadata();
        String repoId = request.getRepository() == null ? "local" : request.getRepository().getId();
        Path file = scratch.resolve("metadata").resolve(repoId)
                .resolve(m.getGroupId()).resolve(m.getArtifactId())
                .resolve(m.getVersion() == null || m.getVersion().isEmpty() ? "" : m.getVersion())
                .resolve("maven-metadata.xml");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file.toFile();
    }

    // A minimal maven-metadata.xml reconstructed from the region entry — only the versioning fields the resolver reads.
    private static byte[] toXml(Metadata coords, MavenMetadata md) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<metadata>\n");
        element(sb, "groupId", coords.getGroupId());
        element(sb, "artifactId", coords.getArtifactId());
        if (coords.getVersion() != null && !coords.getVersion().isEmpty()) {
            element(sb, "version", coords.getVersion());
        }
        MavenMetadata.Versioning v = md.getVersioning();
        if (v != null) {
            sb.append("  <versioning>\n");
            element(sb, "latest", v.getLatest());
            element(sb, "release", v.getRelease());
            if (v.getSnapshot() != null) {
                sb.append("    <snapshot>\n");
                element(sb, "timestamp", v.getSnapshot().getTimestamp());
                element(sb, "buildNumber", v.getSnapshot().getBuildNumber());
                sb.append("    </snapshot>\n");
            }
            if (v.getVersions() != null && !v.getVersions().isEmpty()) {
                sb.append("    <versions>\n");
                for (String ver : v.getVersions()) {
                    element(sb, "version", ver);
                }
                sb.append("    </versions>\n");
            }
            if (v.getSnapshotVersions() != null && !v.getSnapshotVersions().isEmpty()) {
                sb.append("    <snapshotVersions>\n");
                for (MavenMetadata.SnapshotVersion sv : v.getSnapshotVersions()) {
                    sb.append("      <snapshotVersion>\n");
                    element(sb, "classifier", sv.getClassifier());
                    element(sb, "extension", sv.getExtension());
                    element(sb, "value", sv.getValue());
                    element(sb, "updated", sv.getUpdated());
                    sb.append("      </snapshotVersion>\n");
                }
                sb.append("    </snapshotVersions>\n");
            }
            if (v.getLastUpdated() != null) {
                element(sb, "lastUpdated", LAST_UPDATED.format(v.getLastUpdated()));
            }
            sb.append("  </versioning>\n");
        }
        sb.append("</metadata>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void element(StringBuilder sb, String name, @Nullable String value) {
        if (value != null) {
            sb.append("    <").append(name).append('>').append(value).append("</").append(name).append(">\n");
        }
    }

    private static boolean rootedIn(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }
}
