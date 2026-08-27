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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CachingMavenRecipeBundleResolverTest {

    @TempDir
    Path tempDir;

    InMemoryExecutionContext ctx;
    MavenArtifactDownloader downloader;

    @BeforeEach
    void setUp() {
        ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView.view(ctx)
                .setAddCentralRepository(true)
                .setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));
        downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace,
                ctx
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/2346")
    @Test
    void closeDoesNotCloseHandedOutClassLoaders() throws Exception {
        AtomicReference<TrackingRecipeClassLoader> classLoaderRef = new AtomicReference<>();
        CachingMavenRecipeBundleResolver resolver = new CachingMavenRecipeBundleResolver(
                ctx, downloader, (recipeJar, classpath) -> {
            TrackingRecipeClassLoader cl = new TrackingRecipeClassLoader(recipeJar, classpath);
            classLoaderRef.set(cl);
            return cl;
        });

        RecipeBundle bundle = new RecipeBundle(
                "maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);

        RecipeBundleReader reader = resolver.resolve(bundle);
        // Force classloader creation by preparing any one recipe.
        RecipeListing first = reader.read().getAllRecipes().iterator().next();
        reader.prepare(first, null);

        TrackingRecipeClassLoader cl = classLoaderRef.get();
        assertThat(cl)
                .as("classLoaderFactory should have been invoked once prepare() ran")
                .isNotNull();
        assertThat(cl.closeCount).isZero();

        resolver.close();

        assertThat(cl.closeCount)
                .as("Classloaders handed out by the resolver must outlive resolver.close() -- " +
                    "Recipe instances cached upstream (e.g. RunTask.RECIPE_CACHE) retain them, " +
                    "and closing the URLClassPath silently breaks getResourceAsStream/inner-class loads.")
                .isZero();
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/2346")
    @Test
    void resolveWithFreshBundleForSamePackageReusesCachedReader() {
        CachingMavenRecipeBundleResolver resolver = new CachingMavenRecipeBundleResolver(
                ctx, downloader, RecipeClassLoader::new);

        RecipeBundle first = new RecipeBundle(
                "maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
        RecipeBundleReader r1 = resolver.resolve(first);
        r1.read();

        // A fresh RecipeBundle instance for the same package must reuse the cached
        // reader. The prior implementation compared bundle.getVersion() against the
        // (possibly mutated) cached entry and evicted-and-closed the prior reader on
        // any mismatch -- destroying classloaders still in use by Recipe instances
        // cached upstream.
        RecipeBundle freshSameCoordinate = new RecipeBundle(
                "maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
        RecipeBundleReader r2 = resolver.resolve(freshSameCoordinate);

        assertThat(r2).isSameAs(r1);
    }

    private static class TrackingRecipeClassLoader extends RecipeClassLoader {
        int closeCount;

        TrackingRecipeClassLoader(Path recipeJar, List<Path> classpath) {
            super(recipeJar, classpath);
        }

        @Override
        public void close() throws IOException {
            closeCount++;
            super.close();
        }
    }
}
