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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRecipeBundleReaderTest {
    private static final RecipeBundle REWRITE_CORE_BUNDLE =
            new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);

    @TempDir
    Path tempDir;

    MavenRecipeBundleResolver resolver;

    @BeforeEach
    void setUp() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView.view(ctx).setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));
        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null,
                new HttpUrlConnectionSender(),
                Throwable::printStackTrace
        );

        resolver = new MavenRecipeBundleResolver(ctx, downloader, RecipeClassLoader::new);
    }

    @AfterEach
    void tearDown() throws Exception {
        resolver.close();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6487")
    @Test
    void canInstallRewriteCore() {
        RecipeBundleReader reader = resolver.resolve(REWRITE_CORE_BUNDLE);

        RecipeMarketplace marketplace = reader.read();
        assertThat(marketplace.getAllRecipes())
                .isNotEmpty()
                .as("rewrite-core should install and successfully list recipes")
                .anyMatch(r -> r.getName().contains(org.openrewrite.text.Find.class.getName()));
    }

    @Test
    void canDescribeRecipe() {
        RecipeBundleReader reader = resolver.resolve(REWRITE_CORE_BUNDLE);

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

    @Test
    void canPrepareRecipeWithOptions() {
        RecipeBundleReader reader = resolver.resolve(REWRITE_CORE_BUNDLE);

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

    @Test
    void bundleVersionIsSetCorrectly() {
        RecipeBundleReader reader = resolver.resolve(REWRITE_CORE_BUNDLE);

        RecipeMarketplace marketplace = reader.read();

        // Verify all recipes from the bundle have the correct version set
        for (RecipeListing listing : marketplace.getAllRecipes()) {
            RecipeBundle recipeBundle = listing.getBundle();
            if ("org.openrewrite:rewrite-core".equals(recipeBundle.getPackageName())) {
                assertThat(recipeBundle.getVersion()).isEqualTo("8.70.0");
            }
        }
    }

    @Test
    void crossPackageRecipeVersionResolvedFromDependencyTree() throws Exception {
        // Simulate the scenario where a recipe JAR's recipes.csv contains a recipe
        // from a dependency JAR (different packageName). The version for the cross-package
        // recipe should be resolved from the dependency tree.
        //
        // rewrite-static-analysis:2.32.0 has a recipes.csv and depends on
        // org.openrewrite:rewrite-java:8.79.0
        RecipeBundle staticAnalysisBundle = new RecipeBundle("maven",
                "org.openrewrite.recipe:rewrite-static-analysis", "2.32.0", null, null);

        MavenRecipeBundleReader reader = (MavenRecipeBundleReader) resolver.resolve(staticAnalysisBundle);

        // Download the JAR and resolve the full classpath/dependency tree
        List<Path> classpath = reader.classpath();
        Path originalJar = reader.recipeJar;
        assertThat(originalJar).isNotNull().exists();

        // Extract the original JAR
        Path classesDir = tempDir.resolve("modified-classes");
        Files.createDirectories(classesDir);
        MavenRecipeMarketplaceGeneratorTest.extractJar(originalJar, classesDir);

        // Modify recipes.csv to add a cross-package recipe referencing rewrite-java
        // (which is a dependency of rewrite-static-analysis)
        Path recipesCsv = classesDir.resolve("META-INF/rewrite/recipes.csv");
        assertThat(recipesCsv).exists();
        String csvContent = Files.readString(recipesCsv);
        // Match the 11-column header: ecosystem,packageName,name,displayName,description,
        //   recipeCount,category1,category2,category1Description,category2Description,options
        String crossPackageRow = "\nmaven,org.openrewrite:rewrite-java,org.openrewrite.java.CrossPackageTestRecipe,Cross Package Test,A test recipe from a dependency,1,,,,," ;
        Files.writeString(recipesCsv, csvContent + crossPackageRow);

        // Repackage as a new JAR, overwriting the cached artifact in-place
        repackageAsJar(classesDir, originalJar);

        // Reset recipeJar so read() re-enters the download path. The downloader's
        // artifact cache will return the same path (now containing our modified JAR).
        reader.recipeJar = null;

        RecipeMarketplace marketplace = reader.read();

        // Find the cross-package recipe and verify it was attributed to the host JAR
        RecipeListing crossPackageRecipe = marketplace.getAllRecipes().stream()
                .filter(r -> r.getName().equals("org.openrewrite.java.CrossPackageTestRecipe"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cross-package recipe not found in marketplace"));

        // The recipe should be attributed to the host JAR (rewrite-static-analysis),
        // not the dependency JAR (rewrite-java), so the resolver downloads the right artifact
        assertThat(crossPackageRecipe.getBundle().getPackageName())
                .isEqualTo("org.openrewrite.recipe:rewrite-static-analysis");
        assertThat(crossPackageRecipe.getBundle().getVersion())
                .as("Cross-package recipe version should match the host JAR")
                .isNotBlank()
                .isEqualTo("2.32.0");

        reader.close();
    }

    private void repackageAsJar(Path classesDir, Path jarPath) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            Files.walk(classesDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String entryName = classesDir.relativize(path).toString();
                    try {
                        jos.putNextEntry(new JarEntry(entryName));
                        Files.copy(path, jos);
                        jos.closeEntry();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    @Test
    void canReadRecipesFromDirectory() throws Exception {
        MavenRecipeBundleReader reader = (MavenRecipeBundleReader) resolver.resolve(REWRITE_CORE_BUNDLE);

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
                REWRITE_CORE_BUNDLE,
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
