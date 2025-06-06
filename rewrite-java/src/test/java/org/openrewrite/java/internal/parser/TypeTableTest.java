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
package org.openrewrite.java.internal.parser;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParserExecutionContextView;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.micrometer.core.instrument.util.DoubleFormat.decimalOrNan;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class TypeTableTest implements RewriteTest {
    Path tsv;
    ExecutionContext ctx = new InMemoryExecutionContext();

    @TempDir
    Path temp;

    @BeforeEach
    void before() {
        ctx.putMessage(TypeTable.VERIFY_CLASS_WRITING, true);
        JavaParserExecutionContextView.view(ctx).setParserClasspathDownloadTarget(temp.toFile());
        tsv = temp.resolve("types.tsv.zip");
        System.out.println(tsv);
    }

    /**
     * Snappy isn't optimal for compression, but is excellent for portability since it
     * requires no native libraries or JNI.
     *
     * @throws IOException If unable to write.
     */
    @Test
    void writeAllRuntimeClasspathJars() throws IOException {
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
            long jarsSize = 0;
            for (Path classpath : JavaParser.runtimeClasspath()) {
                jarsSize += writeJar(classpath, writer);
            }
            System.out.println("Total size of table " + humanReadableByteCount(Files.size(tsv)));
            System.out.println("Total size of jars " + humanReadableByteCount(jarsSize));
        }
    }

    @Disabled
    @Test
    void writeAllMavenLocal() throws IOException {
        Path m2Repo = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
            AtomicLong jarsSize = new AtomicLong();
            AtomicLong jarCount = new AtomicLong();
            Files.walkFileTree(m2Repo, new SimpleFileVisitor<>() {
                @SneakyThrows
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".jar")) {
                        jarsSize.addAndGet(writeJar(file, writer));
                        if (jarCount.incrementAndGet() > 500) {
                            return FileVisitResult.TERMINATE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("Total size of table " + humanReadableByteCount(Files.size(tsv)));
            System.out.println("Total size of jars " + humanReadableByteCount(jarsSize.get()));
        }
    }

    @Test
    void writeReadJunitJupiterApi() throws IOException {
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
            for (Path classpath : JavaParser.runtimeClasspath()) {
                if (classpath.toFile().getName().contains("junit-jupiter-api")) {
                    writeJar(classpath, writer);
                }
            }
        }

        TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("junit-jupiter-api"));
        Path classesDir = table.load("junit-jupiter-api");
        assertThat(Files.walk(requireNonNull(classesDir))).noneMatch(p -> p.getFileName().toString().endsWith("$1.class"));

        assertThat(classesDir)
          .isNotNull()
          .isDirectoryRecursivelyContaining("glob:**/Assertions.class")
          .isDirectoryRecursivelyContaining("glob:**/BeforeEach.class"); // No fields or methods

        // Demonstrate that the bytecode we wrote for the classes in this
        // JAR is sufficient for the compiler to type attribute code that depends
        // on them.
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpath(List.of(classesDir))),
          java(
            """
              import org.junit.jupiter.api.Assertions;
              import org.junit.jupiter.api.BeforeEach;
              import org.junit.jupiter.api.Test;
              
              class Test {
              
                  @BeforeEach
                  void before() {
                  }
              
                  @Test
                  void foo() {
                      Assertions.assertTrue(true);
                  }
              }
              """
          )
        );
    }

    @Test
    void writeReadWithAnnotations() throws IOException, URISyntaxException {
        URL resource = TypeTableTest.class.getClassLoader().getResource("jakarta.validation-api-3.1.1.jar");
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
            writeJar(Path.of(resource.toURI()), writer);
        }

        TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("jakarta.validation-api"));
        Path classesDir = table.load("jakarta.validation-api");
        assertThat(classesDir).isNotNull();

        // Test that we can use the annotations in code
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpath(List.of(classesDir))),
          java(
            """
              import jakarta.validation.constraints.AssertFalse;
              
              class AnnotatedTest {
                  @AssertFalse
                  int i;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                // Assert that the JavaType.Class has the annotation
                J.VariableDeclarations field = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
                JavaType.Class assertFalseType = (JavaType.Class) field.getLeadingAnnotations().get(0).getType();
                assertThat(assertFalseType).isNotNull();
                assertThat(assertFalseType.getFullyQualifiedName()).isEqualTo("jakarta.validation.constraints.AssertFalse");
                assertThat(assertFalseType.getAnnotations().size()).isEqualTo(5);
                assertThat(assertFalseType.getMethods().stream().filter(m -> m.getName().equals("message"))).satisfiesExactly(
                  m -> assertThat(m.getDefaultValue()).containsExactly("{jakarta.validation.constraints.AssertFalse.message}")
                );
            })
          )
        );
    }

    private static long writeJar(Path classpath, TypeTable.Writer writer) throws IOException {
        String fileName = classpath.toFile().getName();
        if (fileName.endsWith(".jar")) {
            String[] artifactVersion = fileName.replaceAll(".jar$", "")
              .split("-(?=\\d)");
            if (artifactVersion.length > 1) {
                writer
                  .jar("unknown", artifactVersion[0], artifactVersion[1])
                  .write(classpath);
                System.out.println("  Wrote " + artifactVersion[0] + ":" + artifactVersion[1]);
            }
            return Files.size(classpath);
        }
        return 0;
    }

    private String humanReadableByteCount(double bytes) {
        int unit = 1024;
        if (bytes < unit || Double.isNaN(bytes)) {
            return decimalOrNan(bytes) + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return decimalOrNan(bytes / Math.pow(unit, exp)) + " " + pre + "B";
    }

    @Test
    void writeReadWithCustomAnnotations() throws IOException, URISyntaxException {
        // Find the directory containing the compiled classes
        URL classUrl = TypeTableTest.class.getResource("TypeTableTest.class");
        Path classesDir = Paths.get(classUrl.toURI()).getParent().getParent().getParent().getParent().getParent().getParent();
        Path dataDir = classesDir.resolve("org/openrewrite/java/internal/parser/data");

        // Create a temporary jar file
        Path jarFile = temp.resolve("custom-annotations-1.jar");

        // Create jar file from the compiled classes
        try (FileSystem zipfs = FileSystems.newFileSystem(
          jarFile,
          Map.of("create", "true"))) {

            Files.walk(dataDir)
              .filter(Files::isRegularFile)
              .filter(p -> p.toString().endsWith(".class"))
              .forEach(classFile -> {
                  try {
                      Path pathInZip = zipfs.getPath("/").resolve(
                        classesDir.relativize(classFile).toString());
                      Files.createDirectories(pathInZip.getParent());
                      Files.copy(classFile, pathInZip);
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
              });
        }

        // Write the jar to the type table
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
            writeJar(jarFile, writer);
        }

        // Load the type table
        TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("custom-annotations"));
        Path loadedClassesDir = table.load("custom-annotations");
        assertThat(loadedClassesDir).isNotNull();

        // Test that we can use the annotations in code
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpath(List.of(loadedClassesDir))),
          java(
            """
              import org.openrewrite.java.internal.parser.data.*;
              
              @BasicAnnotation
              @StringAnnotation
              @NestedAnnotation
              @ArrayAnnotation
              @ClassRefAnnotation
              @EnumAnnotation
              @ConstantAnnotation
              class TestClass {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                // Verify that all annotations are properly loaded with their attributes
                J.ClassDeclaration clazz = cu.getClasses().get(0);
                List<J.Annotation> annotations = clazz.getLeadingAnnotations();

                // Verify BasicAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.BasicAnnotation",
                  "intValue", "42");

                // Verify StringAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.StringAnnotation",
                  "value", "Default value");

                // Verify ClassRefAnnotation
                // TODO `JavaType.Method#defaultValue` is of type `List<String>`
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.ClassRefAnnotation",
                  "value", "java.lang.String");

                // Verify NestedAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.NestedAnnotation",
                  "nested", "@org.openrewrite.java.internal.parser.data.NestedLevel2");

                // Verify ArrayAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.ArrayAnnotation",
                  "strings", "one", "two", "three");

                // Verify EnumAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.EnumAnnotation",
                  "value", ".ONE");

                // Verify ConstantAnnotation
                verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.ConstantAnnotation",
                  "value", "This is a constant string");
            })
          )
        );
    }

    private void verifyAnnotation(List<J.Annotation> annotations, String fqn, String attributeName, String... expectedValues) {
        J.Annotation annotation = annotations.stream()
          .filter(a -> {
              JavaType.Class type = (JavaType.Class) a.getType();
              return type != null && type.getFullyQualifiedName().equals(fqn);
          })
          .findFirst()
          .orElseThrow(() -> new AssertionError("Annotation " + fqn + " not found"));

        JavaType.Class annotationType = (JavaType.Class) annotation.getType();
        assertThat(annotationType).isNotNull();
        assertThat(annotationType.getMethods().stream()
          .filter(m -> m.getName().equals(attributeName)))
          .satisfiesExactly(
            m -> assertThat(m.getDefaultValue()).containsExactly(expectedValues)
          );
    }
}
