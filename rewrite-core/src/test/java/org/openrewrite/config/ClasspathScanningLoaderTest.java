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
package org.openrewrite.config;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ClasspathScanningLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testInheritanceAcrossJars() throws IOException {
        // Create base jar with abstract Recipe subclass
        Path baseJar = createBaseRecipeJar();

        // Create concrete jar with implementation
        Path concreteJar = createConcreteRecipeJar();

        // Get rewrite-core jar path (contains Recipe base class)
        Path coreJar = findRewriteCoreJar();

        // Test 1: URLClassLoader with all jars - should find concrete recipe
        URLClassLoader fullClassLoader = new URLClassLoader(
          new URL[]{baseJar.toUri().toURL(), concreteJar.toUri().toURL(), coreJar.toUri().toURL()},
          ClasspathScanningLoaderTest.class.getClassLoader() // Use parent for Recipe class identity
        );

        Environment fullEnv = Environment.builder()
          .scanJar(concreteJar, emptyList(), fullClassLoader)
          .build();

        assertThat(fullEnv.listRecipes()).hasSize(1);
        assertThat(fullEnv.listRecipes().getFirst().getName()).isEqualTo("test.ConcreteTestRecipe");

        // Test 2: URLClassLoader missing base jar - should fail to find recipe due to inheritance
        URLClassLoader partialClassLoader = new URLClassLoader(
          new URL[]{concreteJar.toUri().toURL(), coreJar.toUri().toURL()},
          null // No parent to ensure isolation
        );

        Environment partialEnv = Environment.builder()
          .scanJar(concreteJar, emptyList(), partialClassLoader)
          .build();

        // Should find 0 recipes because inheritance chain is broken
        assertThat(partialEnv.listRecipes()).isEmpty();
    }

    private Path createBaseRecipeJar() throws IOException {
        Path baseJar = tempDir.resolve("base-recipe.jar");

        // Create source directory structure
        Path srcDir = tempDir.resolve("base-src");
        Path packageDir = srcDir.resolve("test");
        Files.createDirectories(packageDir);

        // Create abstract Recipe subclass
        @Language("java")
        String abstractRecipeSource = """
          package test;
          import org.openrewrite.Recipe;
          import org.openrewrite.TreeVisitor;
          import org.openrewrite.ExecutionContext;
          import org.openrewrite.SourceFile;
          
          public abstract class AbstractTestRecipe extends Recipe {
              @Override
              public String getDisplayName() {
                  return "Abstract test recipe";
              }
          
              @Override
              public String getDescription() {
                  return "Base class for test recipes.";
              }
          
              @Override
              public abstract TreeVisitor<?, ExecutionContext> getVisitor();
          }
          """;

        Files.write(packageDir.resolve("AbstractTestRecipe.java"), abstractRecipeSource.getBytes());

        // Compile the source using current classpath (includes rewrite-core)
        Path classesDir = tempDir.resolve("base-classes");
        compileJavaWithClasspath(srcDir, classesDir, emptyList());

        // Create jar
        createJar(classesDir, baseJar);

        return baseJar;
    }

    private Path createConcreteRecipeJar() throws IOException {
        Path concreteJar = tempDir.resolve("concrete-recipe.jar");

        // Create source directory structure
        Path srcDir = tempDir.resolve("concrete-src");
        Path packageDir = srcDir.resolve("test");
        Files.createDirectories(packageDir);

        // Create concrete Recipe implementation
        @Language("java")
        String concreteRecipeSource = """
          package test;
          import org.openrewrite.TreeVisitor;
          import org.openrewrite.ExecutionContext;
          import org.openrewrite.SourceFile;
          
          public class ConcreteTestRecipe extends AbstractTestRecipe {
              @Override
              public String getName() {
                  return "test.ConcreteTestRecipe";
              }
          
              @Override
              public String getDisplayName() {
                  return "Concrete test recipe";
              }
          
              @Override
              public String getDescription() {
                  return "Concrete implementation of test recipe.";
              }
          
              @Override
              public TreeVisitor<?, ExecutionContext> getVisitor() {
                  return new TreeVisitor<SourceFile, ExecutionContext>() {};
              }
          }
          """;

        Files.write(packageDir.resolve("ConcreteTestRecipe.java"), concreteRecipeSource.getBytes());

        // Compile the source (need base jar in classpath, current classpath has rewrite-core)
        Path classesDir = tempDir.resolve("concrete-classes");
        Path baseJar = tempDir.resolve("base-recipe.jar");
        compileJavaWithClasspath(srcDir, classesDir, List.of(baseJar));

        // Create jar
        createJar(classesDir, concreteJar);

        return concreteJar;
    }

    private void compileJavaWithClasspath(Path sourceDir, Path outputDir, List<Path> classpathJars) throws IOException {
        Files.createDirectories(outputDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Set output directory
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

        // Use current test classpath plus any additional jars
        var classpath = new ArrayList<Path>();

        // Add current test classpath
        String currentClassPath = System.getProperty("java.class.path");
        Arrays.stream(currentClassPath.split(File.pathSeparator))
          .map(Path::of)
          .forEach(classpath::add);

        // Add additional jars
        classpath.addAll(classpathJars);

        fileManager.setLocation(StandardLocation.CLASS_PATH,
          classpath.stream().map(Path::toFile).toList());

        // Find all Java files
        var javaFiles = fileManager.getJavaFileObjectsFromPaths(
          Files.walk(sourceDir)
            .filter(path -> path.toString().endsWith(".java"))
            .toList()
        );

        // Compile
        var task = compiler.getTask(null, fileManager, null, null, null, javaFiles);
        if (!task.call()) {
            throw new RuntimeException("Compilation failed");
        }

        fileManager.close();
    }

    private void createJar(Path classesDir, Path jarPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Files.walk(classesDir)
              .filter(Files::isRegularFile)
              .forEach(file -> {
                  try {
                      String entryName = classesDir.relativize(file).toString().replace('\\', '/');
                      jos.putNextEntry(new JarEntry(entryName));
                      Files.copy(file, jos);
                      jos.closeEntry();
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
              });
        }
    }

    private Path findRewriteCoreJar() {
        // Try to find rewrite-core jar from current classpath
        String classPath = System.getProperty("java.class.path");
        return Arrays.stream(classPath.split(File.pathSeparator))
          .filter(path -> path.contains("rewrite-core"))
          .filter(path -> path.endsWith(".jar") || path.endsWith("classes"))
          .findFirst()
          .map(Path::of)
          .orElseThrow(() -> new IllegalStateException("Could not find rewrite-core jar or classes in classpath: " + classPath));
    }
}