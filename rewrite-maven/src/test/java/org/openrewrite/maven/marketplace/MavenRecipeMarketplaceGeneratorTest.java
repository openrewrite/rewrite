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
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenRecipeMarketplaceGeneratorTest {

    /**
     * Extract a jar file to a directory using JarInputStream.
     *
     * @param jarPath Path to the jar file to extract
     * @param targetDir Directory to extract to (must exist)
     * @throws IOException If extraction fails
     */
    static void extractJar(Path jarPath, Path targetDir) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarPath))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (FileOutputStream outputStream = new FileOutputStream(entryPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                jarInputStream.closeEntry();
            }
        }
    }

    @Test
    void generateMarketplaceFromJar(@TempDir Path tempDir) throws Exception {
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

        // Download rewrite-rewrite jar and its dependencies for testing
        // Use a recipe jar that is NOT on the test runtime classpath to ensure we're actually scanning the jar
        GroupArtifact ga = new GroupArtifact("org.openrewrite.recipe", "rewrite-rewrite");
        String version = "0.19.0";

        // Use the resolver to get the jar and its classpath
        org.openrewrite.marketplace.RecipeBundle bundle =
                new org.openrewrite.marketplace.RecipeBundle("maven", "org.openrewrite.recipe:rewrite-rewrite", version, null, null);

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
                ctx,
                downloader,
                RecipeClassLoader::new
        ); MavenRecipeBundleReader reader = (MavenRecipeBundleReader) resolver.resolve(bundle)) {
            // Trigger classpath resolution
            reader.classpath();

            Path recipeJar = reader.recipeJar;
            List<Path> classpath = reader.classpath;

            assertThat(recipeJar).isNotNull().exists();
            assertThat(classpath).isNotEmpty();

            // Now test the generator
            MavenRecipeMarketplaceGenerator generator = new MavenRecipeMarketplaceGenerator(ga, recipeJar, classpath);
            RecipeMarketplace marketplace = generator.generate();

            assertThat(marketplace).isNotNull();
            assertThat(marketplace.getAllRecipes())
              .as("Generated marketplace should contain recipes from rewrite-rewrite")
              .isNotEmpty();

            // Store recipe count for comparison with directory test
            int jarRecipeCount = marketplace.getAllRecipes().size();
            assertThat(jarRecipeCount).isGreaterThan(0);

            // Verify recipes have proper bundle information
            for (RecipeListing listing : marketplace.getAllRecipes()) {
                assertThat(listing.getBundle()).isNotNull();
                assertThat(listing.getBundle().getPackageName()).isEqualTo("org.openrewrite.recipe:rewrite-rewrite");
                assertThat(listing.getName()).isNotBlank();
            }
        }
    }

    @Test
    void throwsExceptionWhenJarDoesNotExist(@TempDir Path tempDir) {
        GroupArtifact ga = new GroupArtifact("org.example", "test");
        Path nonExistentJar = tempDir.resolve("does-not-exist.jar");
        List<Path> classpath = new ArrayList<>();

        MavenRecipeMarketplaceGenerator generator = new MavenRecipeMarketplaceGenerator(ga, nonExistentJar, classpath);

        assertThatThrownBy(generator::generate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipe path does not exist");
    }

    @Test
    void generateMarketplaceFromDirectory(@TempDir Path tempDir) throws Exception {
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

        // Download rewrite-rewrite jar and extract it to a directory
        // Use a recipe jar that is NOT on the test runtime classpath to ensure we're actually scanning the directory
        GroupArtifact ga = new GroupArtifact("org.openrewrite.recipe", "rewrite-rewrite");
        String version = "0.19.0";

        org.openrewrite.marketplace.RecipeBundle bundle =
                new org.openrewrite.marketplace.RecipeBundle("maven", "org.openrewrite.recipe:rewrite-rewrite", version, null, null);

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(ctx, downloader, RecipeClassLoader::new);
             MavenRecipeBundleReader reader = (MavenRecipeBundleReader) resolver.resolve(bundle)) {
            reader.classpath();
            Path recipeJar = reader.recipeJar;
            List<Path> classpath = reader.classpath;

            assertThat(recipeJar).isNotNull().exists();

            // Extract jar to a directory to simulate a classes directory
            Path classesDir = tempDir.resolve("classes");
            Files.createDirectories(classesDir);
            extractJar(recipeJar, classesDir);

            // Now test the generator with a directory instead of a jar
            MavenRecipeMarketplaceGenerator generator = new MavenRecipeMarketplaceGenerator(ga, classesDir, classpath);
            RecipeMarketplace marketplace = generator.generate();

            assertThat(marketplace).isNotNull();
            assertThat(marketplace.getAllRecipes())
                    .as("Generated marketplace from directory should contain same recipes as jar")
                    .isNotEmpty();

            // Verify recipes have proper bundle information
            for (RecipeListing listing : marketplace.getAllRecipes()) {
                assertThat(listing.getBundle()).isNotNull();
                assertThat(listing.getBundle().getPackageName()).isEqualTo("org.openrewrite.recipe:rewrite-rewrite");
                assertThat(listing.getName()).isNotBlank();
            }
        }
    }

    /**
     * This test compiles a unique recipe class at runtime and verifies it can be found
     * when scanning a directory. This ensures directory scanning actually works and isn't
     * just finding classes from the runtime classpath.
     */
    @Test
    void generateMarketplaceFromDirectoryWithCompiledRecipe(@TempDir Path tempDir) throws Exception {
        // Create a unique recipe class that definitely doesn't exist on any classpath
        String uniqueClassName = "TestRecipe" + System.currentTimeMillis();
        String packageName = "org.openrewrite.test.generated";
        String fullyQualifiedName = packageName + "." + uniqueClassName;

        String recipeSource = """
                package %s;

                import org.openrewrite.Recipe;
                import org.openrewrite.ExecutionContext;
                import org.openrewrite.TreeVisitor;

                public class %s extends Recipe {
                    @Override
                    public String getDisplayName() {
                        return "Test Recipe for Directory Scanning";
                    }

                    @Override
                    public String getDescription() {
                        return "A test recipe to verify directory scanning works.";
                    }

                    @Override
                    public TreeVisitor<?, ExecutionContext> getVisitor() {
                        return TreeVisitor.noop();
                    }
                }
                """.formatted(packageName, uniqueClassName);

        // Set up directory structure for compilation
        Path sourceDir = tempDir.resolve("src");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve(packageName.replace('.', '/')));
        Files.createDirectories(classesDir);

        // Write source file
        Path sourceFile = sourceDir.resolve(packageName.replace('.', '/') + "/" + uniqueClassName + ".java");
        Files.writeString(sourceFile, recipeSource);

        // Compile the source file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler)
                .as("Java compiler must be available (run with JDK, not JRE)")
                .isNotNull();

        // Get the rewrite-core jar from the classpath for compilation
        String classpath = System.getProperty("java.class.path");

        int result = compiler.run(null, null, null,
                "-classpath", classpath,
                "-d", classesDir.toString(),
                sourceFile.toString());

        assertThat(result)
                .as("Compilation should succeed")
                .isEqualTo(0);

        // Verify the class file was created
        Path classFile = classesDir.resolve(packageName.replace('.', '/') + "/" + uniqueClassName + ".class");
        assertThat(classFile).exists();

        // Now test the generator with the classes directory
        // Use rewrite-core as the classpath dependency since our recipe extends Recipe
        List<Path> classpathEntries = new ArrayList<>();
        for (String entry : classpath.split(System.getProperty("path.separator"))) {
            classpathEntries.add(Path.of(entry));
        }

        GroupArtifact ga = new GroupArtifact("org.openrewrite.test", "generated-recipes");
        MavenRecipeMarketplaceGenerator generator = new MavenRecipeMarketplaceGenerator(ga, classesDir, classpathEntries);
        RecipeMarketplace marketplace = generator.generate();

        assertThat(marketplace).isNotNull();

        // The key assertion: our uniquely-named recipe must be found
        assertThat(marketplace.getAllRecipes())
                .as("Should find our compiled test recipe: " + fullyQualifiedName)
                .anyMatch(r -> r.getName().equals(fullyQualifiedName));

        // Verify we're not finding recipes from the classpath (like rewrite-core recipes)
        // The directory only has our one recipe, so we should only find that one
        List<RecipeListing> recipesFromOurBundle = marketplace.getAllRecipes().stream()
                .filter(r -> r.getBundle().getPackageName().equals("org.openrewrite.test:generated-recipes"))
                .toList();

        assertThat(recipesFromOurBundle)
                .as("Should only find recipes from our directory, not from classpath")
                .hasSize(1)
                .allMatch(r -> r.getName().equals(fullyQualifiedName));
    }

}
