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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParserExecutionContextView;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

import static io.micrometer.core.instrument.util.DoubleFormat.decimalOrNan;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.internal.parser.TypeTable.VERIFY_CLASS_WRITING;

/**
 * Consolidated test suite for TypeTable functionality including annotation escaping,
 * enum constant handling, constant field processing, and integration tests.
 */
@SuppressWarnings("SameParameterValue")
class TypeTableTest implements RewriteTest {

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

    /**
     * Helper method to compile Java source to a class file
     */
    Path compileToClassFile(String source, String className) throws Exception {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        // Extract simple class name for file
        String simpleClassName = className.contains(".") ?
          className.substring(className.lastIndexOf('.') + 1) : className;
        Path sourceFile = srcDir.resolve(simpleClassName + ".java");
        Files.writeString(sourceFile, source);

        int result = compiler.run(null, null, null, "-d", tempDir.toString(), sourceFile.toString());
        assertThat(result).isEqualTo(0);

        return tempDir.resolve(className.replace('.', '/') + ".class");
    }

    /**
     * Helper method to compile multiple Java sources and return their class files
     */
    Path[] compileToClassFiles(String... sourceAndClassPairs) throws Exception {
        if (sourceAndClassPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide source,className pairs");
        }

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        // Write all source files first
        for (int i = 0; i < sourceAndClassPairs.length; i += 2) {
            String source = sourceAndClassPairs[i];
            String className = sourceAndClassPairs[i + 1];
            String simpleClassName = className.contains(".") ?
              className.substring(className.lastIndexOf('.') + 1) : className;
            Path sourceFile = srcDir.resolve(simpleClassName + ".java");
            Files.writeString(sourceFile, source);
        }

        // Compile all sources together so they can reference each other
        Path[] sourceFiles = new Path[sourceAndClassPairs.length / 2];
        for (int i = 0; i < sourceAndClassPairs.length; i += 2) {
            String className = sourceAndClassPairs[i + 1];
            String simpleClassName = className.contains(".") ?
              className.substring(className.lastIndexOf('.') + 1) : className;
            sourceFiles[i / 2] = srcDir.resolve(simpleClassName + ".java");
        }

        String[] compilerArgs = new String[sourceFiles.length + 2];
        compilerArgs[0] = "-d";
        compilerArgs[1] = tempDir.toString();
        for (int i = 0; i < sourceFiles.length; i++) {
            compilerArgs[i + 2] = sourceFiles[i].toString();
        }

        int result = compiler.run(null, null, null, compilerArgs);
        assertThat(result).isEqualTo(0);

        // Return paths to the compiled class files
        Path[] classFiles = new Path[sourceAndClassPairs.length / 2];
        for (int i = 0; i < sourceAndClassPairs.length; i += 2) {
            String className = sourceAndClassPairs[i + 1];
            classFiles[i / 2] = tempDir.resolve(className.replace('.', '/') + ".class");
        }
        return classFiles;
    }

    /**
     * Helper method to create a JAR file from class files
     */
    Path createJarFromClasses(String jarName, Path... classFiles) throws Exception {
        Path jarFile = tempDir.resolve(jarName);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            for (Path classFile : classFiles) {
                String relativePath = tempDir.relativize(classFile).toString();
                JarEntry entry = new JarEntry(relativePath);
                jos.putNextEntry(entry);
                jos.write(Files.readAllBytes(classFile));
                jos.closeEntry();
            }
        }
        return jarFile;
    }

    /**
     * Helper method to process a JAR through TypeTable and return the TSV content
     */
    String processJarThroughTypeTable(Path jarFile, String groupId, String artifactId, String version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TypeTable.Writer writer = TypeTable.newWriter(baos)) {
            writer.jar(groupId, artifactId, version).write(jarFile);
        }

        // Decompress and return TSV content
        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             InputStream gzis = new GZIPInputStream(is);
             java.util.Scanner scanner = new java.util.Scanner(gzis)) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    @Nested
    class AnnotationEscapingTests {

        @Test
        void annotationArraysAreNotDoubleEscaped() throws Exception {
            //language=java
            String annotationSource = """
              package com.example;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @interface TestAnnotation {
                  String[] values();
              }
              """;

            //language=java
            String classSource = """
              package com.example;
              
              @TestAnnotation(values = {"text/plain;charset=UTF-8", "application/json"})
              public class AnnotatedClass {
                  @TestAnnotation(values = {"field", "annotation"})
                  public String field = "test";
              }
              """;

            Path[] classFiles = compileToClassFiles(
              annotationSource, "com.example.TestAnnotation",
              classSource, "com.example.AnnotatedClass"
            );
            Path jarFile = createJarFromClasses("test.jar", classFiles);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify that annotation arrays contain unescaped quotes
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(values=[s\"text/plain;charset=UTF-8\",s\"application/json\"])");
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(values=[s\"field\",s\"annotation\"])");
        }

        @Test
        void controlCharactersAreEscapedInAnnotations() throws Exception {
            //language=java
            String annotationSource = """
              package com.example;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @interface TestAnnotation {
                  String value();
              }
              """;

            //language=java
            String classSource = """
              package com.example;
              
              @TestAnnotation("line1\\nline2\\ttab\\rcarriage")
              public class ControlCharsClass {}
              """;

            Path[] classFiles = compileToClassFiles(
              annotationSource, "com.example.TestAnnotation",
              classSource, "com.example.ControlCharsClass"
            );
            Path jarFile = createJarFromClasses("test.jar", classFiles);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify control characters are properly escaped
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(value=s\"line1\\nline2\\ttab\\rcarriage\")");
        }
    }

    @Nested
    class EnumConstantTests {

        @Test
        void enumConstantsHaveNoConstantValues() throws Exception {
            //language=java
            String enumSource = """
              package com.example;
              
              public enum TestEnum {
                  VALUE1("First"),
                  VALUE2("Second");
              
                  private final String description;
              
                  TestEnum(String description) {
                      this.description = description;
                  }
              
                  public String getDescription() {
                      return description;
                  }
              
                  // Regular constants should still have values
                  public static final String CONSTANT = "test";
                  public static final int NUMBER = 42;
              }
              """;

            Path classFile = compileToClassFile(enumSource, "com.example.TestEnum");
            Path jarFile = createJarFromClasses("test.jar", classFile);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify enum constants don't have constant values (empty last column, now position 17)
            assertThat(tsvContent).contains("VALUE1\tLcom/example/TestEnum;\t\t\t\t\t\t\t");
            assertThat(tsvContent).contains("VALUE2\tLcom/example/TestEnum;\t\t\t\t\t\t\t");

            // Verify regular constants do have values (constantValue now at position 17)
            assertThat(tsvContent).contains("CONSTANT\tLjava/lang/String;\t\t\t\t\t\t\ts\"test\"");
            assertThat(tsvContent).contains("NUMBER\tI\t\t\t\t\t\t\tI42");
        }

        @Test
        void canCompileAgainstTypeTableGeneratedEnum() throws Exception {
            // Create a simple enum that can be compiled against
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_ENUM,
              "TestEnum", "Ljava/lang/Enum<LTestEnum;>;", "java/lang/Enum", null);

            // Enum constant without ConstantValue (this is the fix!)
            FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM,
              "VALUE1", "LTestEnum;", null, null);
            fv.visitEnd();

            // Regular static final field with ConstantValue
            fv = cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
              "CONSTANT", "Ljava/lang/String;", null, "test");
            fv.visitEnd();

            cw.visitEnd();

            Path enumClassFile = tempDir.resolve("TestEnum.class");
            Files.write(enumClassFile, cw.toByteArray());

            // Test compilation against this enum
            //language=java
            String testCode = """
              public class TestUseEnum {
                  public void test() {
                      TestEnum e = TestEnum.VALUE1;
                      String constant = TestEnum.CONSTANT;
                      System.out.println(e + " " + constant);
                  }
              }
              """;

            Path testFile = tempDir.resolve("TestUseEnum.java");
            Files.writeString(testFile, testCode);

            // This should compile successfully without "cannot have a constant value" errors
            int compileResult = compiler.run(null, null, null,
              "-cp", tempDir.toString(),
              testFile.toString());

            assertThat(compileResult)
              .as("Compilation against properly generated enum should succeed")
              .isEqualTo(0);
        }
    }

    @Nested
    class ConstantFieldTests {

        @Test
        void primitiveConstantsAreStored() throws Exception {
            //language=java
            String source = """
              package com.example;
              
              public class Constants {
                  public static final int INT_CONST = 42;
                  public static final long LONG_CONST = 123L;
                  public static final float FLOAT_CONST = 3.14f;
                  public static final double DOUBLE_CONST = 2.718;
                  public static final boolean BOOL_CONST = true;
                  public static final char CHAR_CONST = 'A';
                  public static final byte BYTE_CONST = 1;
                  public static final short SHORT_CONST = 100;
                  public static final String STRING_CONST = "Hello";
              
                  // Non-constant static final fields should not have values
                  public static final Object OBJECT_CONST = new Object();
                  public static final int[] ARRAY_CONST = {1, 2, 3};
              }
              """;

            Path classFile = compileToClassFile(source, "com.example.Constants");
            Path jarFile = createJarFromClasses("test.jar", classFile);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify primitive constants have values (constantValue is now at column 17, with two new columns before it)
            assertThat(tsvContent).contains("INT_CONST\tI\t\t\t\t\t\t\tI42");
            assertThat(tsvContent).contains("LONG_CONST\tJ\t\t\t\t\t\t\tJ123");
            assertThat(tsvContent).contains("FLOAT_CONST\tF\t\t\t\t\t\t\tF3.14");
            assertThat(tsvContent).contains("DOUBLE_CONST\tD\t\t\t\t\t\t\tD2.718");
            assertThat(tsvContent).contains("BOOL_CONST\tZ\t\t\t\t\t\t\tZtrue");
            assertThat(tsvContent).contains("CHAR_CONST\tC\t\t\t\t\t\t\tC65");
            assertThat(tsvContent).contains("BYTE_CONST\tB\t\t\t\t\t\t\tB1");
            assertThat(tsvContent).contains("SHORT_CONST\tS\t\t\t\t\t\t\tS100");
            assertThat(tsvContent).contains("STRING_CONST\tLjava/lang/String;\t\t\t\t\t\t\ts\"Hello\"");

            // Verify non-constant fields don't have values
            assertThat(tsvContent).contains("OBJECT_CONST\tLjava/lang/Object;\t\t\t\t\t\t\t");
            assertThat(tsvContent).contains("ARRAY_CONST\t[I\t\t\t\t\t\t\t");
        }
    }

    @Nested
    class IntegrationTests {

        /**
         * Snappy isn't optimal for compression, but is excellent for portability since it
         * requires no native libraries or JNI.
         */
        @Test
        void writeAllRuntimeClasspathJars() throws Exception {
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
        void writeAllMavenLocal() throws Exception {
            Path m2Repo = Path.of(System.getProperty("user.home"), ".m2", "repository");
            try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
                AtomicLong jarsSize = new AtomicLong();
                AtomicLong jarCount = new AtomicLong();
                Files.walkFileTree(m2Repo, new SimpleFileVisitor<>() {
                    @Override
                    @SneakyThrows
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
        void writeReadJunitJupiterApi() throws Exception {
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
        void writeReadWithAnnotations() throws Exception {
            // Create our own test annotation JAR
            //language=java
            String annotationSource = """
              package test.validation;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.FIELD)
              @interface TestValidation {
                  String message() default "{test.validation.TestValidation.message}";
                  String[] groups() default {};
              }
              """;

            Path[] classFiles = compileToClassFiles(
              annotationSource, "test.validation.TestValidation"
            );
            Path testJar = createJarFromClasses("test-validation.jar", classFiles);

            try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
                writer.jar("test.group", "test-validation", "1.0").write(testJar);
            }

            TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("test-validation"));
            Path classesDir = table.load("test-validation");

            // Verify that TypeTable can successfully load classes from our JAR
            assertThat(classesDir).isNotNull();
            assertThat(classesDir)
              .isDirectoryRecursivelyContaining("glob:**/TestValidation.class");
        }

        @Test
        void annotationAttributeValuesPreservedThroughTypeTableRoundtrip() throws Exception {
            // Create annotation with various default values to test escaping and preservation
            //language=java
            String annotationSource = """
              package test.annotations;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.TYPE, ElementType.FIELD})
              public @interface ValidationRule {
                  String message() default "{validation.rule.message}";
                  String[] values() default {"text/plain;charset=UTF-8", "application/json"};
                  String specialChars() default "line1\\nline2\\ttab\\rcarriage";
                  int priority() default 100;
                  boolean enabled() default true;
              }
              """;

            Path[] classFiles = compileToClassFiles(
              annotationSource, "test.annotations.ValidationRule"
            );
            Path testJar = createJarFromClasses("validation-rules.jar", classFiles);

            // Write through TypeTable
            try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
                writer.jar("test.group", "validation-rules", "1.0").write(testJar);
            }

            // Load back via TypeTable
            TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("validation-rules"));
            Path classesDir = table.load("validation-rules");
            assertThat(classesDir).isNotNull();

            // Test that JavaParser can parse code using the annotation
            // This validates that TypeTable preserved the annotation structure correctly
            rewriteRun(
              spec -> spec.parser((Parser.Builder) JavaParser.fromJavaVersion()
                .classpath(List.of(classesDir)).logCompilationWarningsAndErrors(true)),
              java(
                """
                  import test.annotations.ValidationRule;
                  
                  @ValidationRule
                  class TestClass {
                      @ValidationRule(message = "custom message")
                      private String field;
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    // Verify that the annotation types are properly resolved
                    J.ClassDeclaration clazz = cu.getClasses().getFirst();
                    J.Annotation classAnnotation = clazz.getLeadingAnnotations().getFirst();
                    JavaType.Class annotationType = (JavaType.Class) classAnnotation.getType();

                    assertThat(annotationType).isNotNull();
                    assertThat(annotationType.getFullyQualifiedName()).isEqualTo("test.annotations.ValidationRule");

                    // Verify the annotation has the expected methods (proving structure is preserved)
                    assertThat(annotationType.getMethods()).hasSize(5);
                    assertThat(annotationType.getMethods().stream().map(JavaType.Method::getName))
                      .containsExactlyInAnyOrder("message", "values", "specialChars", "priority", "enabled");

                    // Verify all default values are preserved through the TypeTable roundtrip
                    assertThat(annotationType.getMethods().stream()
                      .filter(m -> "message".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("{validation.rule.message}")
                      );

                    assertThat(annotationType.getMethods().stream()
                      .filter(m -> "values".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("text/plain;charset=UTF-8", "application/json")
                      );

                    assertThat(annotationType.getMethods().stream()
                      .filter(m -> "specialChars".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("line1\nline2\ttab\rcarriage")
                      );

                    assertThat(annotationType.getMethods().stream()
                      .filter(m -> "priority".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("100")
                      );

                    assertThat(annotationType.getMethods().stream()
                      .filter(m -> "enabled".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("true")
                      );

                    // Verify field-level annotation is also properly typed with same default values
                    J.VariableDeclarations field = (J.VariableDeclarations) clazz.getBody().getStatements().getFirst();
                    J.Annotation fieldAnnotation = field.getLeadingAnnotations().getFirst();
                    JavaType.Class fieldAnnotationType = (JavaType.Class) fieldAnnotation.getType();

                    assertThat(fieldAnnotationType).isNotNull();
                    assertThat(fieldAnnotationType.getFullyQualifiedName()).isEqualTo("test.annotations.ValidationRule");

                    // Field annotation should have the same type information (including default values)
                    // even when explicitly overriding some attributes
                    assertThat(fieldAnnotationType.getMethods().stream()
                      .filter(m -> "message".equals(m.getName()))
                      .findFirst())
                      .isPresent()
                      .get()
                      .satisfies(method ->
                        assertThat(method.getDefaultValue()).containsExactly("{validation.rule.message}")
                      );
                })
              )
            );
        }

        @Test
        void typeAndParameterAnnotationsThroughTypeTableRoundtrip() throws Exception {
            // Create a comprehensive set of annotations to test all three annotation types
            //language=java
            String methodAnnotation = """
              package test.annotations;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.METHOD)
              public @interface Transactional {
                  boolean readOnly() default false;
              }
              """;

            //language=java
            String paramAnnotation = """
              package test.annotations;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.PARAMETER)
              public @interface NotNull {
                  String message() default "must not be null";
              }
              """;

            //language=java
            String typeUseAnnotation = """
              package test.annotations;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              public @interface Nullable {}
              """;

            //language=java
            String anotherTypeUseAnnotation = """
              package test.annotations;
              
              import java.lang.annotation.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.TYPE_USE)
              public @interface NonNull {}
              """;

            //language=java
            String libraryClass = """
              package test.library;
              
              import test.annotations.*;
              import java.util.List;
              
              public class AnnotatedLibrary {
                  // Field with type annotation
                  private @Nullable String nullableField;
              
                  // Array field with type annotation
                  private String @NonNull [] nonNullArray;
              
                  // Method with all three annotation types
                  @Transactional(readOnly = true)
                  public @Nullable String processData(
                      @NotNull @NonNull String required,
                      @Nullable List<@NonNull String> items) {
                      return nullableField;
                  }
              
                  // Method with type annotation on generic return type
                  public List<@Nullable String> getNullableStrings() {
                      return null;
                  }
              
                  // Method with complex nested type annotations
                  public void processWildcard(List<? extends @NonNull Number> numbers) {}
              }
              """;

            Path[] classFiles = compileToClassFiles(
              methodAnnotation, "test.annotations.Transactional",
              paramAnnotation, "test.annotations.NotNull",
              typeUseAnnotation, "test.annotations.Nullable",
              anotherTypeUseAnnotation, "test.annotations.NonNull",
              libraryClass, "test.library.AnnotatedLibrary"
            );
            Path testJar = createJarFromClasses("annotated-library.jar", classFiles);

            // Write through TypeTable
            try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(tsv))) {
                writer.jar("test.group", "annotated-library", "1.0").write(testJar);
            }

            // Load back via TypeTable
            TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("annotated-library"));
            Path classesDir = table.load("annotated-library");
            assertThat(classesDir).isNotNull();

            // Verify the generated classes exist
            assertThat(classesDir)
              .isDirectoryRecursivelyContaining("glob:**/Transactional.class")
              .isDirectoryRecursivelyContaining("glob:**/NotNull.class")
              .isDirectoryRecursivelyContaining("glob:**/Nullable.class")
              .isDirectoryRecursivelyContaining("glob:**/NonNull.class")
              .isDirectoryRecursivelyContaining("glob:**/AnnotatedLibrary.class");

            // Test that JavaParser can parse code using the library with all annotation types
            // This validates that TypeTable preserved all annotation information correctly
            rewriteRun(
              spec -> spec.parser((Parser.Builder) JavaParser.fromJavaVersion()
                .classpath(List.of(classesDir)).logCompilationWarningsAndErrors(true)),
              java(
                """
                  import test.library.AnnotatedLibrary;
                  import test.annotations.*;
                  import java.util.List;
                  import java.util.ArrayList;
                  
                  class TestClient {
                      void useLibrary() {
                          AnnotatedLibrary lib = new AnnotatedLibrary();
                  
                          // Call method with all three annotation types
                          String result = lib.processData("required", new ArrayList<>());
                  
                          // Call method with type annotations on return type
                          List<String> nullables = lib.getNullableStrings();
                  
                          // Call method with complex type annotations
                          List<Integer> numbers = new ArrayList<>();
                          lib.processWildcard(numbers);
                      }
                  
                      // Use the annotations in our own code to verify they work
                      @Transactional
                      public void transactionalMethod(@NotNull String param) {}
                  
                      private @Nullable String nullableField;
                  
                      public List<@NonNull String> getRequiredStrings() {
                          return new ArrayList<>();
                      }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    // Basic verification that the compilation succeeded
                    J.ClassDeclaration clazz = cu.getClasses().getFirst();
                    assertThat(clazz.getSimpleName()).isEqualTo("TestClient");

                    // Find the transactionalMethod
                    J.MethodDeclaration transactionalMethod = clazz.getBody().getStatements().stream()
                      .filter(J.MethodDeclaration.class::isInstance)
                      .map(J.MethodDeclaration.class::cast)
                      .filter(m -> "transactionalMethod".equals(m.getSimpleName()))
                      .findFirst()
                      .orElseThrow();

                    // Verify the method has the @Transactional annotation
                    assertThat(transactionalMethod.getLeadingAnnotations()).hasSize(1);
                    J.Annotation transactionalAnn = transactionalMethod.getLeadingAnnotations().getFirst();
                    assertThat(transactionalAnn.getSimpleName()).isEqualTo("Transactional");

                    // Verify the annotation type is resolved
                    JavaType.Class annotationType = (JavaType.Class) transactionalAnn.getType();
                    assertThat(annotationType).isNotNull();
                    assertThat(annotationType.getFullyQualifiedName()).isEqualTo("test.annotations.Transactional");

                    // Note: As you mentioned, JavaType model might not yet fully support
                    // parameter annotations and type annotations, so we can't verify those
                    // in detail. But the fact that compilation succeeds proves that
                    // TypeTable preserved enough information for the compiler to work.
                })
              )
            );
        }

    }

    // Helper methods for integration tests
    private static long writeJar(Path classpath, TypeTable.Writer writer) throws Exception {
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
}
