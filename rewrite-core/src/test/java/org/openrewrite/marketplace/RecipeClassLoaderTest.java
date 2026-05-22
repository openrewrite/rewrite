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
package org.openrewrite.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.V1_8;

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
    void kotlinClassesShouldDelegateToParent(@TempDir Path tempDir) throws Exception {
        // Reproduces moderneinc/customer-requests#2372: when both the parent and the
        // recipe classloader can define a kotlin.* class (e.g., kotlin.jvm.functions.Function1),
        // the JVM raises LinkageError on loader-constraint violations once Jackson, loaded
        // from the parent, interacts with jackson-module-kotlin from the recipe jar.
        // The fix is to delegate kotlin.* classes to the parent, mirroring slf4j/jackson.
        Path parentLib = tempDir.resolve("parent");
        Files.createDirectories(parentLib.resolve("kotlin/jvm/functions"));
        Files.write(parentLib.resolve("kotlin/jvm/functions/Function1.class"),
          stubClass("kotlin/jvm/functions/Function1"));

        Path childLib = tempDir.resolve("child");
        Files.createDirectories(childLib.resolve("kotlin/jvm/functions"));
        Files.write(childLib.resolve("kotlin/jvm/functions/Function1.class"),
          stubClass("kotlin/jvm/functions/Function1"));

        try (URLClassLoader parent = new URLClassLoader(new URL[]{parentLib.toUri().toURL()}, null);
             RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{childLib.toUri().toURL()}, parent)) {
            Class<?> loaded = classLoader.loadClass("kotlin.jvm.functions.Function1");
            assertThat(loaded.getClassLoader())
              .as("kotlin.* classes must come from the parent to avoid LinkageError")
              .isSameAs(parent);
        }
    }

    @Test
    void kotlinClassesFallBackToChildWhenParentLacksThem(@TempDir Path tempDir) throws Exception {
        // The parent-first delegation must still fall back to the child if the parent
        // does not provide the kotlin class, so recipes can ship their own kotlin runtime
        // when the host application has none.
        Path childLib = tempDir.resolve("child");
        Files.createDirectories(childLib.resolve("kotlin"));
        Files.write(childLib.resolve("kotlin/Standalone.class"),
          stubClass("kotlin/Standalone"));

        try (URLClassLoader parent = new URLClassLoader(new URL[0], null);
             RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{childLib.toUri().toURL()}, parent)) {
            Class<?> loaded = classLoader.loadClass("kotlin.Standalone");
            assertThat(loaded.getClassLoader()).isSameAs(classLoader);
        }
    }

    private static byte[] stubClass(String internalName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    void kotlinRecipeDslClassesShouldBeChildLoaded(@TempDir Path tempDir) throws Exception {
        // Reproduces moderneinc/moderne-cli#3949: `org.openrewrite.RecipeBuilder` collides with
        // the `org.openrewrite.Recipe` allowlist entry under startsWith() matching, which would
        // route it to the parent classloader. That conflicts with the lambda receivers
        // (EditScope, ScanScope) — those don't match any prefix and are child-loaded —
        // producing a ClassCastException on the first getVisitor() call. The Kotlin
        // recipe-DSL builder must come from the recipe jar so all DSL types resolve through
        // one loader.
        Path parentLib = tempDir.resolve("parent");
        Files.createDirectories(parentLib.resolve("org/openrewrite"));
        Files.write(parentLib.resolve("org/openrewrite/RecipeBuilder.class"),
          stubClass("org/openrewrite/RecipeBuilder"));
        Files.write(parentLib.resolve("org/openrewrite/RecipeBuilder$buildSimpleRecipe$1.class"),
          stubClass("org/openrewrite/RecipeBuilder$buildSimpleRecipe$1"));
        Files.write(parentLib.resolve("org/openrewrite/RecipeDslKt.class"),
          stubClass("org/openrewrite/RecipeDslKt"));

        Path childLib = tempDir.resolve("child");
        Files.createDirectories(childLib.resolve("org/openrewrite"));
        Files.write(childLib.resolve("org/openrewrite/RecipeBuilder.class"),
          stubClass("org/openrewrite/RecipeBuilder"));
        Files.write(childLib.resolve("org/openrewrite/RecipeBuilder$buildSimpleRecipe$1.class"),
          stubClass("org/openrewrite/RecipeBuilder$buildSimpleRecipe$1"));
        Files.write(childLib.resolve("org/openrewrite/RecipeDslKt.class"),
          stubClass("org/openrewrite/RecipeDslKt"));

        try (URLClassLoader parent = new URLClassLoader(new URL[]{parentLib.toUri().toURL()}, null);
             RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{childLib.toUri().toURL()}, parent)) {
            assertThat(classLoader.loadClass("org.openrewrite.RecipeBuilder").getClassLoader())
              .as("RecipeBuilder must be child-loaded for DSL receiver/constructor agreement")
              .isSameAs(classLoader);
            assertThat(classLoader.loadClass("org.openrewrite.RecipeBuilder$buildSimpleRecipe$1").getClassLoader())
              .as("RecipeBuilder's synthetic inner classes must share the loader of their host")
              .isSameAs(classLoader);
            assertThat(classLoader.loadClass("org.openrewrite.RecipeDslKt").getClassLoader())
              .as("RecipeDslKt (top-level fn holder) must be child-loaded")
              .isSameAs(classLoader);
        }
    }

    @Test
    void recipeFrameworkClassesStillDelegateToParent(@TempDir Path tempDir) throws Exception {
        // The NON_DELEGATED_CLASSES override is narrow: framework types under the
        // `org.openrewrite.Recipe*` umbrella that aren't DSL internals — RecipeException,
        // RecipeRun, RecipeSerializer, etc. — must remain parent-delegated so exceptions
        // thrown across the loader boundary are catchable and result objects round-trip.
        Path parentLib = tempDir.resolve("parent");
        Files.createDirectories(parentLib.resolve("org/openrewrite"));
        Files.write(parentLib.resolve("org/openrewrite/RecipeException.class"),
          stubClass("org/openrewrite/RecipeException"));

        Path childLib = tempDir.resolve("child");
        Files.createDirectories(childLib.resolve("org/openrewrite"));
        Files.write(childLib.resolve("org/openrewrite/RecipeException.class"),
          stubClass("org/openrewrite/RecipeException"));

        try (URLClassLoader parent = new URLClassLoader(new URL[]{parentLib.toUri().toURL()}, null);
             RecipeClassLoader classLoader = new RecipeClassLoader(new URL[]{childLib.toUri().toURL()}, parent)) {
            assertThat(classLoader.loadClass("org.openrewrite.RecipeException").getClassLoader())
              .as("RecipeException stays parent-delegated under the Recipe-prefix entry")
              .isSameAs(parent);
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
