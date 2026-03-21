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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CachingMavenRecipeBundleResolver implements RecipeBundleResolver {
    private final ExecutionContext ctx;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;
    private final Map<String, ResolverEntry> resolverCache = new HashMap<>();

    @Override
    public String getEcosystem() {
        return "maven";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        try (RecipeBundleResolver resolver = resolverFor(bundle)) {
            return resolver.resolve(bundle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolverKey(RecipeBundle bundle) {
        return String.format("%s:%s:%s", bundle.getTeam(), bundle.getPackageEcosystem(), bundle.getPackageName());
    }

    public synchronized RecipeBundleResolver resolverFor(RecipeBundle bundle) {
        String key = resolverKey(bundle);
        ResolverEntry entry = resolverCache.get(key);
        if (entry != null && !entry.evicted) {
            if (Objects.equals(entry.bundle.getVersion(), bundle.getVersion())) {
                return entry.createProxyResolver();
            }

            entry.markEvicted();
        }

        entry = new ResolverEntry(bundle, new MavenRecipeBundleResolver(ctx, downloader, classLoaderFactory));
        resolverCache.put(key, entry);
        return entry.createProxyResolver();
    }

    @RequiredArgsConstructor
    private static class ResolverEntry {
        private final RecipeBundle bundle;
        private final RecipeBundleResolver resolver;
        private final AtomicInteger leases = new AtomicInteger();
        private boolean evicted;

        private synchronized void markEvicted() {
            evicted = true;
            if (leases.get() == 0) {
                try {
                    resolver.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        RecipeBundleResolver createProxyResolver() {
            leases.incrementAndGet();
            return new RecipeBundleResolver() {
                @Override
                public String getEcosystem() {
                    return resolver.getEcosystem();
                }

                @Override
                public RecipeBundleReader resolve(RecipeBundle bundle) {
                    return resolver.resolve(bundle);
                }

                @Override
                public void close() throws Exception {
                    synchronized (ResolverEntry.this) {
                        if (leases.decrementAndGet() == 0 && evicted) {
                            resolver.close();
                        }
                    }
                }
            };
        }
    }
}
