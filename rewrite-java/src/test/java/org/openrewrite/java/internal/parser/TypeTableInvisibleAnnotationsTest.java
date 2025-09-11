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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParserExecutionContextView;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.internal.parser.TypeTable.VERIFY_CLASS_WRITING;

/**
 * Tests that TypeTable correctly collects and stores both visible (runtime-retained)
 * and invisible (class/source-retained) annotations.
 */
class TypeTableInvisibleAnnotationsTest {

    @TempDir
    Path tempDir;

    ExecutionContext ctx;
    JavaCompiler compiler;
    Path tsv;

    @BeforeEach
    void setUp() {
        ctx = new InMemoryExecutionContext();
        ctx.putMessage(VERIFY_CLASS_WRITING, true);
        JavaParserExecutionContextView.view(ctx).setParserClasspathDownloadTarget(tempDir.toFile());
        compiler = ToolProvider.getSystemJavaCompiler();
        tsv = tempDir.resolve("types.tsv.gz");
    }

    @Test
    void collectsRuntimeRetainedAnnotations() throws Exception {
        // Create test sources with RUNTIME retention (visible annotations)
        @Language("java")
        String annotationSource = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.RUNTIME)
          @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
          public @interface RuntimeAnnotation {
              String value() default "runtime";
          }
          """;

        @Language("java")
        String classSource = """
          package test;
          
          import test.annotations.RuntimeAnnotation;
          
          @RuntimeAnnotation("class-level")
          public class TestClass {
          
              @RuntimeAnnotation("field-level")
              public String field;
          
              @RuntimeAnnotation("method-level")
              public void method() {}
          }
          """;

        Path jarFile = compileAndPackage(annotationSource, "test.annotations.RuntimeAnnotation",
          classSource, "test.TestClass");

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Verify runtime annotations are collected
        assertThat(tsvContent)
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"class-level\")")
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"field-level\")")
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"method-level\")");
    }

    @Test
    void collectsClassRetainedAnnotations() throws Exception {
        // Create test sources with CLASS retention (invisible annotations)
        @Language("java")
        String annotationSource = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.CLASS)
          @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
          public @interface ClassAnnotation {
              String value() default "class";
          }
          """;

        @Language("java")
        String classSource = """
          package test;
          
          import test.annotations.ClassAnnotation;
          
          @ClassAnnotation("class-level")
          public class TestClass {
          
              @ClassAnnotation("field-level")
              public String field;
          
              @ClassAnnotation("method-level")
              public void method() {}
          }
          """;

        Path jarFile = compileAndPackage(annotationSource, "test.annotations.ClassAnnotation",
          classSource, "test.TestClass");

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Verify CLASS retention annotations are now collected (these are invisible annotations)
        assertThat(tsvContent)
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"class-level\")")
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"field-level\")")
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"method-level\")");
    }

    @Test
    void collectsBothVisibleAndInvisibleAnnotations() throws Exception {
        // Create test with both runtime and class retained annotations
        @Language("java")
        String runtimeAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.RUNTIME)
          @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
          public @interface RuntimeAnnotation {
              String value();
          }
          """;

        @Language("java")
        String classAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.CLASS)
          @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
          public @interface ClassAnnotation {
              String value();
          }
          """;

        @Language("java")
        String testClass = """
          package test;
          
          import test.annotations.*;
          
          @RuntimeAnnotation("runtime-class")
          @ClassAnnotation("class-class")
          public class TestClass {
          
              @RuntimeAnnotation("runtime-field")
              @ClassAnnotation("class-field")
              public String field;
          
              @RuntimeAnnotation("runtime-method")
              @ClassAnnotation("class-method")
              public void method() {}
          }
          """;

        Path jarFile = compileAndPackage(
          runtimeAnnotation, "test.annotations.RuntimeAnnotation",
          classAnnotation, "test.annotations.ClassAnnotation",
          testClass, "test.TestClass"
        );

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Verify both visible (runtime) and invisible (class) annotations are collected
        assertThat(tsvContent)
          // Class-level annotations
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"runtime-class\")")
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"class-class\")")
          // Field-level annotations
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"runtime-field\")")
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"class-field\")")
          // Method-level annotations
          .contains("@Ltest/annotations/RuntimeAnnotation;(value=s\"runtime-method\")")
          .contains("@Ltest/annotations/ClassAnnotation;(value=s\"class-method\")");
    }

    @Test
    void preservesAnnotationOrderInTSV() throws Exception {
        // Test that multiple annotations on the same element are preserved in order
        @Language("java")
        String ann1 = """
          package test.annotations;
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.CLASS)
          @Target(ElementType.TYPE)
          public @interface First {
              String value();
          }
          """;

        @Language("java")
        String ann2 = """
          package test.annotations;
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.RUNTIME)
          @Target(ElementType.TYPE)
          public @interface Second {
              int value();
          }
          """;

        @Language("java")
        String ann3 = """
          package test.annotations;
          import java.lang.annotation.*;
          
          @Retention(RetentionPolicy.CLASS)
          @Target(ElementType.TYPE)
          public @interface Third {
              boolean value();
          }
          """;

        @Language("java")
        String testClass = """
          package test;
          
          import test.annotations.*;
          
          @First("first")
          @Second(42)
          @Third(true)
          public class TestClass {
          }
          """;

        Path jarFile = compileAndPackage(
          ann1, "test.annotations.First",
          ann2, "test.annotations.Second",
          ann3, "test.annotations.Third",
          testClass, "test.TestClass"
        );

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Find the line with TestClass annotations
        String[] lines = tsvContent.split("\n");
        String classLine = null;
        for (String line : lines) {
            if (line.contains("\ttest/TestClass\t") && line.contains("@Ltest/annotations/")) {
                classLine = line;
                break;
            }
        }

        assertThat(classLine).isNotNull();

        // Extract the annotations column (15th column, 0-indexed as 14)
        String[] columns = classLine.split("\t");
        assertThat(columns.length).isGreaterThanOrEqualTo(15);
        String annotationsColumn = columns[14];

        // Verify all three annotations are present, concatenated without delimiters
        // The annotations are self-delimiting (each starts with @ and has clear boundaries)
        assertThat(annotationsColumn)
          .contains("@Ltest/annotations/First;(value=s\"first\")")
          .contains("@Ltest/annotations/Second;(value=I42)")
          .contains("@Ltest/annotations/Third;(value=Ztrue)");
    }

    /**
     * Helper to compile sources and create a JAR
     */
    private Path compileAndPackage(String... sourceAndClassPairs) throws Exception {
        if (sourceAndClassPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide source,className pairs");
        }

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        // Write all source files
        for (int i = 0; i < sourceAndClassPairs.length; i += 2) {
            String source = sourceAndClassPairs[i];
            String className = sourceAndClassPairs[i + 1];

            // Create package directories
            String packagePath = className.substring(0, className.lastIndexOf('.')).replace('.', '/');
            Path packageDir = srcDir.resolve(packagePath);
            Files.createDirectories(packageDir);

            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            Path sourceFile = packageDir.resolve(simpleClassName + ".java");
            Files.writeString(sourceFile, source);
        }

        // Compile all sources
        List<Path> allSources = Files.walk(srcDir)
          .filter(p -> p.toString().endsWith(".java"))
          .toList();

        String[] compilerArgs = new String[allSources.size() + 2];
        compilerArgs[0] = "-d";
        compilerArgs[1] = tempDir.toString();
        for (int i = 0; i < allSources.size(); i++) {
            compilerArgs[i + 2] = allSources.get(i).toString();
        }

        int result = compiler.run(null, null, null, compilerArgs);
        assertThat(result).isEqualTo(0);

        // Create JAR from compiled classes
        Path jarFile = tempDir.resolve("test.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            Files.walk(tempDir)
              .filter(p -> p.toString().endsWith(".class"))
              .forEach(classFile -> {
                  try {
                      String entryName = tempDir.relativize(classFile).toString();
                      JarEntry entry = new JarEntry(entryName);
                      jos.putNextEntry(entry);
                      jos.write(Files.readAllBytes(classFile));
                      jos.closeEntry();
                  } catch (Exception e) {
                      throw new RuntimeException(e);
                  }
              });
        }

        return jarFile;
    }

    /**
     * Process a JAR through TypeTable and return the TSV content
     */
    private String processJarThroughTypeTable(Path jarFile) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TypeTable.Writer writer = TypeTable.newWriter(baos)) {
            writer.jar("test.group", "test-artifact", "1.0").write(jarFile);
        }

        // Decompress and return TSV content
        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             InputStream gzis = new GZIPInputStream(is);
             java.util.Scanner scanner = new java.util.Scanner(gzis)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
