package org.openrewrite.maven.cache;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.internal.MavenDownloader;
import org.openrewrite.maven.tree.GroupArtifact;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class InMemoryCache implements MavenCache {
    private final Map<String, Optional<RawMaven>> pomCache = new HashMap<>();
    private final Map<GroupArtifactRepository, Optional<MavenMetadata>> mavenMetadataCache = new HashMap<>();
    private final Map<RawPom.Repository, Optional<RawPom.Repository>> normalizedRepositoryUrls = new HashMap<>();

    CacheResult<RawMaven> UNAVAILABLE_POM = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<MavenMetadata> UNAVAILABLE_METADATA = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<RawPom.Repository> UNAVAILABLE_REPOSITORY = new CacheResult<>(CacheResult.State.Unavailable, null);

    public InMemoryCache() {
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "poms"), pomCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "metadata"), mavenMetadataCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "repository urls"), normalizedRepositoryUrls);
        fillUnresolvablePoms();
    }

    private void fillUnresolvablePoms() {
        new BufferedReader(new InputStreamReader(MavenDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))
                .lines()
                .filter(line -> !line.isEmpty())
                .forEach(gav -> pomCache.put(gav, Optional.empty()));
    }

    @Override
    public CacheResult<MavenMetadata> computeMavenMetadata(URL repo, String groupId, String artifactId, Callable<MavenMetadata> orElseGet) throws Exception {
        GroupArtifactRepository gar = new GroupArtifactRepository(repo, new GroupArtifact(groupId, artifactId));
        Optional<MavenMetadata> rawMavenMetadata = mavenMetadataCache.get(gar);

        //noinspection OptionalAssignedToNull
        if (rawMavenMetadata == null) {
            try {
                MavenMetadata metadata = orElseGet.call();
                mavenMetadataCache.put(gar, Optional.ofNullable(metadata));
                return new CacheResult<>(CacheResult.State.Updated, metadata);
            } catch (Exception e) {
                mavenMetadataCache.put(gar, Optional.empty());
                throw e;
            }
        }

        return rawMavenMetadata
                .map(metadata -> new CacheResult<>(CacheResult.State.Cached, metadata))
                .orElse(UNAVAILABLE_METADATA);
    }

    @Override
    public CacheResult<RawMaven> computeMaven(URL repo, String groupId, String artifactId, String version,
                                              Callable<RawMaven> orElseGet) throws Exception {
        // FIXME key be repo as well, because different repos may have different versions of the same POM
        String cacheKey = groupId + ':' + artifactId + ':' + version;
        Optional<RawMaven> rawMaven = pomCache.get(cacheKey);

        //noinspection OptionalAssignedToNull
        if (rawMaven == null) {
            try {
                RawMaven maven = orElseGet.call();
                pomCache.put(cacheKey, Optional.ofNullable(maven));
                return new CacheResult<>(CacheResult.State.Updated, maven);
            } catch (Exception e) {
                pomCache.put(cacheKey, Optional.empty());
                throw e;
            }
        }

        return rawMaven
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_POM);
    }

    @Override
    public CacheResult<RawPom.Repository> computeRepository(RawPom.Repository repository,
                                                            Callable<RawPom.Repository> orElseGet) throws Exception {
        Optional<RawPom.Repository> normalizedRepository = normalizedRepositoryUrls.get(repository);

        //noinspection OptionalAssignedToNull
        if (normalizedRepository == null) {
            try {
                RawPom.Repository repo = orElseGet.call();
                normalizedRepositoryUrls.put(repository, Optional.ofNullable(repo));
                return new CacheResult<>(CacheResult.State.Updated, repo);
            } catch (Exception e) {
                normalizedRepositoryUrls.put(repository, Optional.empty());
                throw e;
            }
        }

        return normalizedRepository
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_REPOSITORY);
    }
}
