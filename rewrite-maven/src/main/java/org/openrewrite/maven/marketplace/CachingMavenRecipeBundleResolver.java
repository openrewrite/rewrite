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
package org.openrewrite.maven.marketplace;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeClassLoaderFactory;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.util.HashMap;
import java.util.Map;

/**
 * Deduplicates {@link MavenRecipeBundleResolver} instances per package coordinate so that
 * repeated {@link #resolve(RecipeBundle)} calls for the same package share one reader
 * (and therefore one {@link org.openrewrite.marketplace.RecipeClassLoader}).
 * <p>
 * <strong>Lifetime of handed-out readers.</strong> The readers (and the
 * {@link org.openrewrite.marketplace.RecipeClassLoader}s they own) returned by
 * {@link #resolve(RecipeBundle)} are intended to outlive this resolver. Once a {@link org.openrewrite.Recipe}
 * has been prepared from a reader, the JVM's class cache contains classes defined by that
 * classloader, and any later {@code getResourceAsStream}/inner-class load against those
 * classes requires the classloader's {@code URLClassPath} jar handles to remain open.
 * <p>
 * For that reason {@link #close()} only clears the deduplication cache. It does <em>not</em>
 * close the cached inner {@link MavenRecipeBundleResolver}s -- doing so would close their
 * readers, which would close the underlying {@code URLClassLoader}s while consumers still
 * hold references. The visible failure mode is inner-class {@code NoClassDefFoundError} and
 * silently-null {@code getResourceAsStream} returns on a classloader that {@code ==} identity
 * confirms is still alive (see
 * <a href="https://github.com/moderneinc/customer-requests/issues/2346">customer-requests#2346</a>).
 * <p>
 * Consumers that need deterministic cleanup must close the {@link RecipeBundleReader}s they
 * retain (or rely on JVM exit for one-shot processes).
 */
@RequiredArgsConstructor
public class CachingMavenRecipeBundleResolver implements RecipeBundleResolver {
    private final ExecutionContext ctx;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;
    private final Map<String, MavenRecipeBundleResolver> resolverCache = new HashMap<>();

    @Override
    public String getEcosystem() {
        return "maven";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        return resolverFor(bundle).resolve(bundle);
    }

    private String resolverKey(RecipeBundle bundle) {
        return String.format("%s:%s:%s", bundle.getTeam(), bundle.getPackageEcosystem(), bundle.getPackageName());
    }

    public synchronized RecipeBundleResolver resolverFor(RecipeBundle bundle) {
        // Deduplicate by package coordinate only. A version comparison here used to evict
        // the cached entry whenever a caller passed a fresh RecipeBundle still on the
        // requested version (e.g., "0.5.9-SNAPSHOT") while the cached entry had been
        // mutated to the dated snapshot ("0.5.9-20260512.123000") by the first resolve.
        // That eviction destructively closed the prior reader's classloader -- which was
        // still in use by Recipe instances cached upstream. We accept that a second
        // resolve with a different version returns the first resolver's reader; callers
        // that need version pinning resolve into separate resolverKey()s (different
        // packageNames) already.
        return resolverCache.computeIfAbsent(resolverKey(bundle),
                k -> new MavenRecipeBundleResolver(ctx, downloader, classLoaderFactory));
    }

    @Override
    public void close() {
        // Intentionally a no-op. Cached resolvers own readers/classloaders that may be
        // retained externally and must outlive this resolver. See the class-level javadoc.
        // Consumers responsible for deterministic cleanup must close their retained
        // RecipeBundleReader instances directly.
    }
}
