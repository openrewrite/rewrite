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
import org.openrewrite.Recipe;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeClassLoaderTest {

    @Test
    void loadRecipeWithIsolatedClassLoader() {
        // Find rewrite-java JAR in the classpath
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);

        Path recipeJar = null;
        List<Path> dependencies = new ArrayList<>();

        for (String entry : classpathEntries) {
            Path path = Paths.get(entry);
            String fileName = path.getFileName().toString();

            if (fileName.startsWith("rewrite-java-") && fileName.endsWith(".jar") && !fileName.contains("test")) {
                recipeJar = path;
            }
            if (fileName.startsWith("rewrite-") && fileName.endsWith(".jar")) {
                dependencies.add(path);
            }
        }

        if (recipeJar == null || dependencies.isEmpty()) {
            // Skip test if we can't find the JAR (e.g., running in IDE without built JARs)
            return;
        }

        Environment env = Environment.builder()
          .load(new ClasspathScanningLoader(new Properties(), new RecipeClassLoader(recipeJar, dependencies)))
          .build();

        // Get any recipe descriptor from the environment
        RecipeDescriptor descriptor = env.listRecipeDescriptors().stream()
          .findFirst()
          .orElse(null);

        assertThat(descriptor).isNotNull();

        Recipe recipe = env.activateRecipes(descriptor.getName());
        assertThat(recipe).isNotNull();
        assertThat(recipe.getName()).isEqualTo(descriptor.getName());

        assertThat(recipe.getClass().getClassLoader())
          .as("Recipe should be loaded through RecipeClassLoader for isolation")
          .isInstanceOf(RecipeClassLoader.class);
    }

    @Test
    void resourcesShouldBeChildLoaded(@TempDir Path tempDir) throws IOException {
        Path lib1 = tempDir.resolve("lib1");
        Files.createDirectories(lib1);
        Path file1 = lib1.resolve("rewrite.txt");
        Files.write(file1, "file1".getBytes());
        Path lib2 = tempDir.resolve("lib2");
        Files.createDirectories(lib2);
        Path file2 = lib2.resolve("rewrite.txt");
        Files.write(file2, "file2".getBytes());

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{lib1.toUri().toURL()})) {
            try (RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{lib2.toUri().toURL()}, urlClassLoader)) {
                String text = StringUtils.readFully(classLoader.getResourceAsStream("rewrite.txt"));
                assertThat(text).isEqualTo("file2");
            }
        }
    }

    @Test
    void resourcesShouldFindFromParentLast(@TempDir Path tempDir) throws IOException {
        Path lib1 = tempDir.resolve("lib1");
        Files.createDirectories(lib1);
        Path file1 = lib1.resolve("rewrite.txt");
        Files.write(file1, "file1".getBytes());
        Path lib2 = tempDir.resolve("lib2");
        Files.createDirectories(lib2);

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{lib1.toUri().toURL()})) {
            try (RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{lib2.toUri().toURL()}, urlClassLoader)) {
                String text = StringUtils.readFully(classLoader.getResourceAsStream("rewrite.txt"));
                assertThat(text).isEqualTo("file1");
            }
        }
    }

    @Test
    void allResourcesShouldFindFromChildFirst(@TempDir Path tempDir) throws IOException {
        Path lib1 = tempDir.resolve("lib1");
        Files.createDirectories(lib1);
        Path file1 = lib1.resolve("rewrite.txt");
        Files.write(file1, "file1".getBytes());
        Path lib2 = tempDir.resolve("lib2");
        Files.createDirectories(lib2);
        Path file2 = lib2.resolve("rewrite.txt");
        Files.write(file2, "file2".getBytes());

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{lib1.toUri().toURL()})) {
            try (RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{lib2.toUri().toURL()}, urlClassLoader)) {
                Enumeration<URL> resources = classLoader.getResources("rewrite.txt");
                assertThat(resources.hasMoreElements()).isTrue();
                assertThat(resources.nextElement().toString()).contains("lib2/rewrite.txt");
                assertThat(resources.hasMoreElements()).isTrue();
                assertThat(resources.nextElement().toString()).contains("lib1/rewrite.txt");
                assertThat(resources.hasMoreElements()).isFalse();
            }
        }
    }

    @Test
    void allResourcesShouldFindFromParentLast(@TempDir Path tempDir) throws IOException {
        Path lib1 = tempDir.resolve("lib1");
        Files.createDirectories(lib1);
        Path file1 = lib1.resolve("rewrite.txt");
        Files.write(file1, "file1".getBytes());
        Path lib2 = tempDir.resolve("lib2");
        Files.createDirectories(lib2);

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{lib1.toUri().toURL()})) {
            try (RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{lib2.toUri().toURL()}, urlClassLoader)) {
                Enumeration<URL> resources = classLoader.getResources("rewrite.txt");
                assertThat(resources.hasMoreElements()).isTrue();
                assertThat(resources.nextElement().toString()).contains("lib1/rewrite.txt");
                assertThat(resources.hasMoreElements()).isFalse();

            }
        }
    }
}
