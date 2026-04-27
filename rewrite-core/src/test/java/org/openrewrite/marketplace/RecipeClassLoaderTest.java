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
import org.openrewrite.Recipe;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.StringUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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
    void nestmatesFollowHostOnChildLoader(@TempDir Path tempDir) throws Exception {
        Path nestDir = compileSyntheticNest(tempDir);
        URL[] urls = {nestDir.toUri().toURL()};

        // Both child and parent can see the full nest. Configure the child to delegate the
        // private inner to the parent — without the NestHost-aware fix this would split the
        // nest (Outer + Outer$1 on child, Outer$Inner on parent) and produce IllegalAccessError.
        try (URLClassLoader parent = new URLClassLoader(urls, getClass().getClassLoader())) {
            try (RecipeClassLoader child = new TestRecipeClassLoader(urls, parent,
              Collections.singletonList("nest.Outer$Inner"))) {

                Class<?> outer = child.loadClass("nest.Outer");
                @SuppressWarnings("unchecked")
                Callable<Integer> caller = (Callable<Integer>) outer.getMethod("caller").invoke(null);
                assertThat(caller.call()).isEqualTo(42);

                assertThat(outer.getClassLoader()).isSameAs(child);
                assertThat(caller.getClass().getClassLoader()).isSameAs(child);
                assertThat(child.loadClass("nest.Outer$Inner").getClassLoader()).isSameAs(child);
            }
        }
    }

    @Test
    void nestmatesFollowHostOnParentLoader(@TempDir Path tempDir) throws Exception {
        Path nestDir = compileSyntheticNest(tempDir);
        URL[] urls = {nestDir.toUri().toURL()};

        // Delegate the host to the parent. With the fix, the anonymous and inner classes
        // peek their NestHost, see that Outer was defined by the parent, and route there too.
        try (URLClassLoader parent = new URLClassLoader(urls, getClass().getClassLoader())) {
            try (RecipeClassLoader child = new TestRecipeClassLoader(urls, parent,
              Collections.singletonList("nest.Outer"))) {

                Class<?> outer = child.loadClass("nest.Outer");
                @SuppressWarnings("unchecked")
                Callable<Integer> caller = (Callable<Integer>) outer.getMethod("caller").invoke(null);
                assertThat(caller.call()).isEqualTo(42);

                assertThat(outer.getClassLoader()).isSameAs(parent);
                Class<?> anon = child.loadClass("nest.Outer$1");
                Class<?> inner = child.loadClass("nest.Outer$Inner");
                assertThat(anon.getClassLoader()).isSameAs(parent);
                assertThat(inner.getClassLoader()).isSameAs(parent);
            }
        }
    }

    @Test
    void nestSplitFailsLoudlyWhenMemberMissing(@TempDir Path tempDir) throws Exception {
        Path fullNest = compileSyntheticNest(tempDir);

        // Build a child class directory that intentionally omits Outer$Inner.
        Path childDir = tempDir.resolve("child-classes");
        Path childPkg = childDir.resolve("nest");
        Files.createDirectories(childPkg);
        Files.copy(fullNest.resolve("nest/Outer.class"), childPkg.resolve("Outer.class"));
        Files.copy(fullNest.resolve("nest/Outer$1.class"), childPkg.resolve("Outer$1.class"));

        URL[] childUrls = {childDir.toUri().toURL()};
        URL[] parentUrls = {fullNest.toUri().toURL()};

        try (URLClassLoader parent = new URLClassLoader(parentUrls, getClass().getClassLoader())) {
            try (RecipeClassLoader child = new RecipeClassLoader(childUrls, parent)) {
                Class<?> outer = child.loadClass("nest.Outer");
                assertThat(outer.getClassLoader()).isSameAs(child);

                // Outer$Inner is absent from the child. Pre-fix the loader would silently fall
                // back to the parent, defining the inner there and producing IllegalAccessError
                // when Outer$1 instantiates it. Post-fix the host check refuses that fallback
                // and the missing member surfaces as ClassNotFoundException.
                Throwable thrown = catchThrowable(() -> child.loadClass("nest.Outer$Inner"));
                assertThat(thrown).isInstanceOf(ClassNotFoundException.class);

                @SuppressWarnings("unchecked")
                Callable<Integer> caller = (Callable<Integer>) outer.getMethod("caller").invoke(null);
                assertThatThrownBy(caller::call)
                  .isInstanceOfAny(NoClassDefFoundError.class, ClassNotFoundException.class);
            }
        }
    }

    /**
     * Compiles a tiny nest into {@code tempDir/nest-classes}:
     * <ul>
     *     <li>{@code nest.Outer} — host</li>
     *     <li>{@code nest.Outer$1} — anonymous {@link Callable} returning {@code new Inner().value()}</li>
     *     <li>{@code nest.Outer$Inner} — private static, private {@code value()} returning 42</li>
     * </ul>
     * Targets release 11 so the resulting class files carry NestHost/NestMembers attributes.
     */
    private static Path compileSyntheticNest(Path tempDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Tests require running on a JDK with javac available");
        }

        Path srcDir = tempDir.resolve("nest-src");
        Path pkgDir = srcDir.resolve("nest");
        Files.createDirectories(pkgDir);
        Path src = pkgDir.resolve("Outer.java");
        Files.write(src, ("package nest;\n" +
          "import java.util.concurrent.Callable;\n" +
          "public class Outer {\n" +
          "    public static Callable<Integer> caller() {\n" +
          "        return new Callable<Integer>() {\n" +
          "            @Override public Integer call() { return new Inner().value(); }\n" +
          "        };\n" +
          "    }\n" +
          "    private static class Inner {\n" +
          "        private int value() { return 42; }\n" +
          "    }\n" +
          "}\n").getBytes());

        Path classDir = tempDir.resolve("nest-classes");
        Files.createDirectories(classDir);

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(classDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(
              Collections.singletonList(src.toFile()));
            boolean ok = compiler.getTask(null, fm, null,
              Arrays.asList("--release", "11"), null, units).call();
            if (!ok) {
                throw new IllegalStateException("Failed to compile synthetic nest");
            }
        }
        return classDir;
    }

    private static final class TestRecipeClassLoader extends RecipeClassLoader {
        private final List<String> additionalDelegated;

        TestRecipeClassLoader(URL[] urls, ClassLoader parent, List<String> additionalDelegated) {
            super(urls, parent);
            this.additionalDelegated = additionalDelegated;
        }

        @Override
        protected List<String> getAdditionalParentDelegatedPackages() {
            return additionalDelegated;
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
