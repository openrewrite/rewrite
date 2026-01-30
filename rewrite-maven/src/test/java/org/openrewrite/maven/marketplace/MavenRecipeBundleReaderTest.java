/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRecipeBundleReaderTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/6487")
    @Test
    void canInstallRewriteCore(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
          artifactCache,
          null,
          new HttpUrlConnectionSender(),
          Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
          ctx,
          downloader,
          RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            RecipeBundleReader reader = resolver.resolve(bundle);

            RecipeMarketplace marketplace = reader.read();
            assertThat(marketplace.getAllRecipes())
              .isNotEmpty()
              .as("rewrite-core should install and successfully list recipes")
              .anyMatch(r -> r.getName().contains(org.openrewrite.text.Find.class.getName()));
        }
    }

    @Test
    void canDescribeRecipe(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
                ctx,
                downloader,
                RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            RecipeBundleReader reader = resolver.resolve(bundle);

            RecipeMarketplace marketplace = reader.read();
            RecipeListing findRecipe = marketplace.getAllRecipes().stream()
                    .filter(r -> r.getName().equals(org.openrewrite.text.Find.class.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Find recipe not found"));

            RecipeDescriptor descriptor = reader.describe(findRecipe);
            assertThat(descriptor).isNotNull();
            assertThat(descriptor.getName()).isEqualTo(org.openrewrite.text.Find.class.getName());
            assertThat(descriptor.getDisplayName()).isNotBlank();
        }
    }

    @Test
    void canPrepareRecipeWithOptions(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
                ctx,
                downloader,
                RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            RecipeBundleReader reader = resolver.resolve(bundle);

            RecipeMarketplace marketplace = reader.read();
            RecipeListing findRecipe = marketplace.getAllRecipes().stream()
                    .filter(r -> r.getName().equals(org.openrewrite.text.Find.class.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Find recipe not found"));

            Map<String, Object> options = new HashMap<>();
            options.put("find", "test");

            Recipe recipe = reader.prepare(findRecipe, options);
            assertThat(recipe).isNotNull();
            assertThat(recipe.getName()).isEqualTo(org.openrewrite.text.Find.class.getName());
        }
    }

    @Test
    void bundleVersionIsSetCorrectly(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
                ctx,
                downloader,
                RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            RecipeBundleReader reader = resolver.resolve(bundle);

            RecipeMarketplace marketplace = reader.read();

            // Verify all recipes from the bundle have the correct version set
            for (RecipeListing listing : marketplace.getAllRecipes()) {
                RecipeBundle recipeBundle = listing.getBundle();
                if ("org.openrewrite:rewrite-core".equals(recipeBundle.getPackageName())) {
                    assertThat(recipeBundle.getVersion()).isEqualTo("8.70.0");
                }
            }
        }
    }

    @Test
    void canReadRecipesFromDirectory(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
                ctx,
                downloader,
                RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            MavenRecipeBundleReader reader = (MavenRecipeBundleReader) resolver.resolve(bundle);

            // Trigger classpath resolution to get the jar
            List<Path> classpath = reader.classpath();
            Path recipeJar = reader.recipeJar;

            assertThat(recipeJar).isNotNull().exists();

            // Extract jar to a directory to simulate a classes directory
            Path classesDir = tempDir.resolve("classes");
            Files.createDirectories(classesDir);
            MavenRecipeMarketplaceGeneratorTest.extractJar(recipeJar, classesDir);

            // Close the original reader
            reader.close();

            // Create a new reader pointing to the directory instead of the jar
            MavenRecipeBundleReader dirReader = new MavenRecipeBundleReader(
                    bundle,
                    null, null,
                    RecipeClassLoader::new
            );

            dirReader.recipeJar = classesDir;
            dirReader.classpath = classpath;

            // Read marketplace from directory
            RecipeMarketplace marketplace = dirReader.marketplaceFromClasspathScan();

            assertThat(marketplace.getAllRecipes())
                    .as("Should find same recipes when scanning directory as when scanning jar")
                    .hasSize(27)
                    .anyMatch(r -> r.getName().contains(org.openrewrite.text.Find.class.getName()));

            dirReader.close();
        }
    }
}
