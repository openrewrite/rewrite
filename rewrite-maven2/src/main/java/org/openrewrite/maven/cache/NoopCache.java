package org.openrewrite.maven.cache;

import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenMetadata;
import org.openrewrite.maven.internal.RawPom;

import java.net.URL;
import java.util.concurrent.Callable;

public class NoopCache implements MavenCache {
    @Override
    public CacheResult<RawMavenMetadata> computeMavenMetadata(URL repo, String groupId, String artifactId, Callable<RawMavenMetadata> orElseGet) throws Exception {
        return new CacheResult<>(CacheResult.State.Updated, orElseGet.call());
    }

    @Override
    public CacheResult<RawMaven> computeMaven(URL repo, String groupId, String artifactId, String version, Callable<RawMaven> orElseGet) throws Exception {
        return new CacheResult<>(CacheResult.State.Updated, orElseGet.call());
    }

    @Override
    public CacheResult<RawPom.Repository> computeRepository(RawPom.Repository repository, Callable<RawPom.Repository> orElseGet) throws Exception {
        return new CacheResult<>(CacheResult.State.Updated, orElseGet.call());
    }
}
