package org.openrewrite.maven.cache;

import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenMetadata;
import org.openrewrite.maven.internal.RawPom;

import java.io.Closeable;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface MavenCache extends AutoCloseable {
    CacheResult<RawMavenMetadata> computeMavenMetadata(URL repo,
                                                       String groupId,
                                                       String artifactId,
                                                       Callable<RawMavenMetadata> orElseGet) throws Exception;

    CacheResult<RawMaven> computeMaven(URL repo,
                                       String groupId,
                                       String artifactId,
                                       String version,
                                       Callable<RawMaven> orElseGet) throws Exception;

    /**
     * Store a normalized repository given an input repository. Normalization takes, for example,
     * an http:// scheme repository URL for a repository that now requires https and changes the scheme.
     */
    CacheResult<RawPom.Repository> computeRepository(RawPom.Repository repository,
                                                     Callable<RawPom.Repository> orElseGet) throws Exception;
}
