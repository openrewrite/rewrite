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
 * Tests for the new TSV format with three annotation columns:
 * elementAnnotations, parameterAnnotations, and typeAnnotations
 */
@SuppressWarnings("resource")
class TypeTableTypeAnnotationsTest {

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
    void capturesParameterAnnotations() throws Exception {
        @Language("java")
        String paramAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.PARAMETER)
          @Retention(RetentionPolicy.RUNTIME)
          public @interface NotNull {}
          """;

        @Language("java")
        String validAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.PARAMETER)
          @Retention(RetentionPolicy.CLASS)
          public @interface Valid {}
          """;

        @Language("java")
        String testClass = """
          package test;
          
          import test.annotations.*;
          
          public class TestClass {
              public void singleParam(@NotNull String param) {}
          
              public void multipleParams(@NotNull String first, @Valid @NotNull Object second) {}
          
              public void mixedParams(String plain, @Valid Object annotated) {}
          }
          """;

        Path jarFile = compileAndPackage(
          paramAnnotation, "test.annotations.NotNull",
          validAnnotation, "test.annotations.Valid",
          testClass, "test.TestClass"
        );

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Parse TSV to check parameter annotations column (column 16)
        String[] lines = tsvContent.split("\n");

        // Find method rows
        for (String line : lines) {
            if (line.contains("\tsingleParam\t")) {
                String[] cols = line.split("\t", -1);
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[15]).as("parameterAnnotations column for singleParam")
                  .isEqualTo("@Ltest/annotations/NotNull;");
            }

            if (line.contains("\tmultipleParams\t")) {
                String[] cols = line.split("\t", -1);
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[15]).as("parameterAnnotations column for multipleParams")
                  .isEqualTo("@Ltest/annotations/NotNull;|@Ltest/annotations/NotNull;@Ltest/annotations/Valid;");
            }

            if (line.contains("\tmixedParams\t")) {
                String[] cols = line.split("\t", -1);
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[15]).as("parameterAnnotations column for mixedParams")
                  .isEqualTo("|@Ltest/annotations/Valid;");
            }
        }
    }

    @Test
    void capturesTypeAnnotations() throws Exception {
        @Language("java")
        String nullableAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
          @Retention(RetentionPolicy.RUNTIME)
          public @interface Nullable {}
          """;

        @Language("java")
        String nonNullAnnotation = """
          package test.annotations;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.TYPE_USE)
          @Retention(RetentionPolicy.RUNTIME)
          public @interface NonNull {}
          """;

        @Language("java")
        String testClass = """
          package test;
          
          import test.annotations.*;
          import java.util.List;
          
          public class TestClass {
              // Type annotation on field
              @Nullable String field;
          
              // Type annotations on array
              String @NonNull [] arrayField;
          
              // Type annotation on method return
              @Nullable String getField() { return field; }
          
              // Type annotation on method parameter type
              void setField(@NonNull String value) { this.field = value; }
          
              // Complex: generic with type annotations
              void processList(List<@Nullable String> list) {}
          
              // Complex: wildcard with type annotation
              void processWildcard(List<? extends @NonNull Number> numbers) {}
          }
          """;

        Path jarFile = compileAndPackage(
          nullableAnnotation, "test.annotations.Nullable",
          nonNullAnnotation, "test.annotations.NonNull",
          testClass, "test.TestClass"
        );

        String tsvContent = processJarThroughTypeTable(jarFile);

        // Parse TSV to check type annotations column (column 17)
        String[] lines = tsvContent.split("\n");

        for (String line : lines) {
            String[] cols = line.split("\t", -1);

            if (line.contains("\tfield\t") && line.contains("Ljava/lang/String;")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for field")
                  .contains("13000000::@Ltest/annotations/Nullable;");
            }

            if (line.contains("\tarrayField\t")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for arrayField")
                  .contains("13000000::@Ltest/annotations/NonNull;");
            }

            if (line.contains("\tgetField\t")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for getField")
                  .contains("14000000::@Ltest/annotations/Nullable;");
            }

            if (line.contains("\tsetField\t")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for setField")
                  .contains("16000000::@Ltest/annotations/NonNull;");
            }

            if (line.contains("\tprocessList\t")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for processList")
                  .contains("16000000:0;:@Ltest/annotations/Nullable;");
            }

            if (line.contains("\tprocessWildcard\t")) {
                assertThat(cols.length).isGreaterThanOrEqualTo(18);
                assertThat(cols[16]).as("typeAnnotations for processWildcard")
                  .contains("16000000:0;*:@Ltest/annotations/NonNull;");
            }
        }
    }

    @Test
    void columnOrderCorrect() throws Exception {
        // Simple test to verify column order is as expected
        @Language("java")
        String testClass = """
          package test;
          
          @Deprecated
          public class TestClass {
              @Deprecated
              public static final String CONSTANT = "value";
          
              @Deprecated
              public void method() {}
          }
          """;

        Path jarFile = compileAndPackage(testClass, "test.TestClass");
        String tsvContent = processJarThroughTypeTable(jarFile);

        String[] lines = tsvContent.split("\n");
        String header = lines[0].trim();

        // Verify header has the right columns in order
        assertThat(header).isEqualTo(
          "groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\t" +
            "classSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\t" +
            "descriptor\tsignature\tparameterNames\texceptions\telementAnnotations\t" +
            "parameterAnnotations\ttypeAnnotations\tconstantValue"
        );

        // Find the field row and check constantValue is in the right place
        for (String line : lines) {
            if (line.contains("\tCONSTANT\t")) {
                String[] cols = line.split("\t", -1);
                assertThat(cols.length).isEqualTo(18);
                assertThat(cols[14]).as("elementAnnotations").contains("@Ljava/lang/Deprecated;");
                assertThat(cols[15]).as("parameterAnnotations").isEmpty();
                assertThat(cols[16]).as("typeAnnotations").isEmpty();
                assertThat(cols[17].trim()).as("constantValue").isEqualTo("s\"value\"");
            }
        }
    }

    @Test
    void annotationsWithSpecialCharactersInValues() throws Exception {
        // Test that annotation values with special characters are properly escaped
        // and don't break the TSV format
        @Language("java")
        String sources = """
          package test;
          
          import java.lang.annotation.*;
          
          @Target({ElementType.PARAMETER, ElementType.TYPE_USE})
          @Retention(RetentionPolicy.RUNTIME)
          @interface Message {
              String value();
              String[] tags() default {};
          }
          
          @Target(ElementType.TYPE_USE)
          @Retention(RetentionPolicy.RUNTIME)
          @interface Format {
              String pattern();
          }
          
          public class TestClass {
              // Test string with quotes that need escaping
              public void quotes(@Message(value = "He said \\"Hello\\"") String param) {}
          
              // Test string with commas (delimiter in arrays)
              public void commas(@Message(value = "a,b,c", tags = {"tag1,tag2", "tag3|tag4"}) String param) {}
          
              // Test string with pipe characters (delimiter between annotations)
              public void pipes(@Message(value = "value|with|pipes") String param) {}
          
              // Test string with newlines and tabs
              public void whitespace(@Message(value = "line1\\nline2\\ttab") String param) {}
          
              // Test string with backslashes
              public void backslashes(@Message(value = "C:\\\\Users\\\\file.txt") String param) {}
          
              // Type annotation with special characters
              public @Format(pattern = "\\\\d{2,4}-\\\\d{2}-\\\\d{2}|\\\\d{4}/\\\\d{2}/\\\\d{2}") String getDate() {
                  return null;
              }
          
              // Multiple annotations with complex values
              public void complex(
                  @Message(value = "Complex: \\"a,b|c\\\\d\\n\\"", tags = {"t1|t2", "t3,t4"})
                  @Format(pattern = "[a-z]+|[A-Z]+")
                  String param
              ) {}
          }
          """;

        Path jarFile = compileAndPackage(sources, "test.TestClass");
        String tsvContent = processJarThroughTypeTable(jarFile);

        // Parse the TSV content to verify it's valid
        String[] lines = tsvContent.split("\n");
        for (String line : lines) {
            String[] cols = line.split("\t", -1);

            if (line.contains("\tquotes\t")) {
                // Check that the escaped quotes are properly handled
                assertThat(cols[15]).as("parameterAnnotations for quotes method")
                  .contains("@Ltest/Message;")
                  .contains("value=s\"He said \\\"Hello\\\"\"");
            }

            if (line.contains("\tcommas\t")) {
                // Check that commas in values don't break the format
                // Note: pipes are escaped in TSV format since they're used as delimiters  
                assertThat(cols[15]).as("parameterAnnotations for commas method")
                  .contains("@Ltest/Message;")
                  .contains("value=s\"a,b,c\"")
                  .contains("tags=[s\"tag1,tag2\",s\"tag3\\|tag4\"]");
            }

            if (line.contains("\tpipes\t")) {
                // Check that pipes in values are escaped (pipes are TSV delimiters)
                assertThat(cols[15]).as("parameterAnnotations for pipes method")
                  .contains("value=s\"value\\|with\\|pipes\"");
                // Also verify that the pipe delimiter between parameters is handled correctly
                // if there were multiple parameters with annotations
            }

            if (line.contains("\twhitespace\t")) {
                // Check that newlines and tabs are escaped
                assertThat(cols[15]).as("parameterAnnotations for whitespace method")
                  .contains("value=s\"line1\\nline2\\ttab\"");
            }

            if (line.contains("\tbackslashes\t")) {
                // Check that backslashes are properly escaped
                assertThat(cols[15]).as("parameterAnnotations for backslashes method")
                  .contains("value=s\"C:\\\\Users\\\\file.txt\"");
            }

            if (line.contains("\tgetDate\t")) {
                // Check type annotation with regex pattern (pipes and backslashes are escaped in TSV)
                assertThat(cols[16]).as("typeAnnotations for getDate method")
                  .contains("14000000::@Ltest/Format;")
                  .contains("pattern=s\"\\\\d{2,4}-\\\\d{2}-\\\\d{2}\\|\\\\d{4}/\\\\d{2}/\\\\d{2}\"");
            }

            if (line.contains("\tcomplex\t")) {
                // Check multiple annotations with complex values
                // @Message is a parameter annotation, @Format is a type annotation
                assertThat(cols[15]).as("parameterAnnotations for complex method")
                  .contains("@Ltest/Message;");
                assertThat(cols[16]).as("typeAnnotations for complex method")
                  .contains("16000000::@Ltest/Format;");
            }
        }
    }

    @Test
    void parameterAnnotationsWithPipeDelimiters() throws Exception {
        // Test that pipe characters in parameter annotation values don't break
        // the pipe delimiter between different parameters
        @Language("java")
        String sources = """
          package test;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.PARAMETER)
          @Retention(RetentionPolicy.RUNTIME)
          @interface Pattern {
              String value();
          }
          
          @Target(ElementType.PARAMETER)
          @Retention(RetentionPolicy.RUNTIME)
          @interface Description {
              String text();
          }
          
          public class TestClass {
              // Multiple parameters with annotations containing pipe characters
              public void validate(
                  @Pattern("\\\\d+|\\\\w+") String first,
                  @Description(text = "Options: A|B|C") String second,
                  @Pattern("[a-z]+|[A-Z]+") @Description(text = "Letters|Digits") String third
              ) {}
          }
          """;

        Path jarFile = compileAndPackage(sources, "test.TestClass");
        String tsvContent = processJarThroughTypeTable(jarFile);

        // Find the validate method row
        for (String line : tsvContent.split("\n")) {
            if (line.contains("\tvalidate\t")) {
                String[] cols = line.split("\t", -1);
                String paramAnnotations = cols[15];

                // Verify the parameter annotations are properly formatted
                // The pipe delimiter between parameters should work correctly
                // even though the annotation values contain escaped pipes
                assertThat(paramAnnotations).as("parameterAnnotations for validate method")
                  .contains("@Ltest/Pattern;(value=s\"\\\\d+\\|\\\\w+\")")
                  .contains("@Ltest/Description;(text=s\"Options: A\\|B\\|C\")")
                  .contains("@Ltest/Pattern;(value=s\"[a-z]+\\|[A-Z]+\")@Ltest/Description;(text=s\"Letters\\|Digits\")");

                break;
            }
        }
    }

    @Test
    void allThreeAnnotationTypesOnMethod() throws Exception {
        // Test a method that has element annotations, parameter annotations, and type annotations
        @Language("java")
        String sources = """
          package test;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.METHOD)
          @Retention(RetentionPolicy.RUNTIME)
          @interface MethodAnnotation {}
          
          @Target(ElementType.PARAMETER)
          @Retention(RetentionPolicy.RUNTIME)
          @interface ParamAnnotation {}
          
          @Target(ElementType.TYPE_USE)
          @Retention(RetentionPolicy.RUNTIME)
          @interface TypeAnnotation {}
          
          public class TestClass {
              @MethodAnnotation
              public @TypeAnnotation String process(@ParamAnnotation @TypeAnnotation String input) {
                  return input;
              }
          }
          """;

        Path jarFile = compileAndPackage(sources, "test.TestClass");
        String tsvContent = processJarThroughTypeTable(jarFile);

        // Find the process method row
        for (String line : tsvContent.split("\n")) {
            if (line.contains("\tprocess\t")) {
                String[] cols = line.split("\t", -1);
                assertThat(cols[14]).as("elementAnnotations")
                  .contains("@Ltest/MethodAnnotation;");
                assertThat(cols[15]).as("parameterAnnotations")
                  .contains("@Ltest/ParamAnnotation;");
                assertThat(cols[16]).as("typeAnnotations")
                  .contains("14000000::@Ltest/TypeAnnotation;")
                  .contains("16000000::@Ltest/TypeAnnotation;");
                break;
            }
        }
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
            String packagePath = className.contains(".") ?
              className.substring(0, className.lastIndexOf('.')).replace('.', '/') : "";
            if (!packagePath.isEmpty()) {
                Path packageDir = srcDir.resolve(packagePath);
                Files.createDirectories(packageDir);
            }

            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            Path sourceFile = packagePath.isEmpty() ?
              srcDir.resolve(simpleClassName + ".java") :
              srcDir.resolve(packagePath).resolve(simpleClassName + ".java");
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
