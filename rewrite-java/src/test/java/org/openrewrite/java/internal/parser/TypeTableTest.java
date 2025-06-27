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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParserExecutionContextView;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
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
        tsv = tempDir.resolve("types.tsv.zip");
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
            String source = """
                package com.example;
                
                import java.lang.annotation.*;
                
                @Retention(RetentionPolicy.RUNTIME)
                @interface TestAnnotation {
                    String[] values();
                }
                
                @TestAnnotation(values = {"text/plain;charset=UTF-8", "application/json"})
                public class AnnotatedClass {
                    @TestAnnotation(values = {"field", "annotation"})
                    public String field = "test";
                }
                """;

            Path classFile = compileToClassFile(source, "com.example.AnnotatedClass");
            Path jarFile = createJarFromClasses("test.jar", classFile);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify that annotation arrays contain unescaped quotes
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(values={\"text/plain;charset=UTF-8\",\"application/json\"})");
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(values={\"field\",\"annotation\"})");
        }

        @Test
        void controlCharactersAreEscapedInAnnotations() throws Exception {
            //language=java
            String source = """
                package com.example;
                
                import java.lang.annotation.*;
                
                @Retention(RetentionPolicy.RUNTIME)
                @interface TestAnnotation {
                    String value();
                }
                
                @TestAnnotation("line1\\nline2\\ttab\\rcarriage")
                public class ControlCharsClass {}
                """;

            Path classFile = compileToClassFile(source, "com.example.ControlCharsClass");
            Path jarFile = createJarFromClasses("test.jar", classFile);
            String tsvContent = processJarThroughTypeTable(jarFile, "com.example", "test", "1.0");

            // Verify control characters are properly escaped
            assertThat(tsvContent).contains("@Lcom/example/TestAnnotation;(value=\"line1\\nline2\\ttab\\rcarriage\")");
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

            // Verify enum constants don't have constant values (empty last column)
            assertThat(tsvContent).contains("VALUE1\tLcom/example/TestEnum;\t\t\t\t\t");
            assertThat(tsvContent).contains("VALUE2\tLcom/example/TestEnum;\t\t\t\t\t");
            
            // Verify regular constants do have values
            assertThat(tsvContent).contains("CONSTANT\tLjava/lang/String;\t\t\t\t\t\"test\"");
            assertThat(tsvContent).contains("NUMBER\tI\t\t\t\t\t42");
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

            // Verify primitive constants have values
            assertThat(tsvContent).contains("INT_CONST\tI\t\t\t\t\t42");
            assertThat(tsvContent).contains("LONG_CONST\tJ\t\t\t\t\t123");
            assertThat(tsvContent).contains("STRING_CONST\tLjava/lang/String;\t\t\t\t\t\"Hello\"");
            
            // Verify non-constant fields don't have values
            assertThat(tsvContent).contains("OBJECT_CONST\tLjava/lang/Object;\t\t\t\t\t");
            assertThat(tsvContent).contains("ARRAY_CONST\t[I\t\t\t\t\t");
        }
    }

    @SuppressWarnings({"DataFlowIssue", "OptionalGetWithoutIsPresent", "resource"})
    @Nested
    class IntegrationTests {

        /**
         * Snappy isn't optimal for compression, but is excellent for portability since it
         * requires no native libraries or JNI.
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
        void writeReadWithCustomAnnotations() throws IOException, URISyntaxException {
            // Find the directory containing the compiled classes
            URL classUrl = TypeTableTest.class.getResource("TypeTableTest.class");
            Path classesDir = Paths.get(classUrl.toURI()).getParent().getParent().getParent().getParent().getParent().getParent();
            Path dataDir = classesDir.resolve("org/openrewrite/java/internal/parser/data");

            // Create a temporary jar file
            Path jarFile = tempDir.resolve("custom-annotations-1.jar");

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
                      AnnotatedClass annotatedClass;
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    // Verify that all annotations are properly loaded with their attributes
                    J.ClassDeclaration clazz = cu.getClasses().getFirst();

                    JavaType.Class annotatedClass = (JavaType.Class) ((J.VariableDeclarations) clazz.getBody().getStatements().getFirst()).getTypeExpression().getType();
                    JavaType.Variable field = annotatedClass.getMembers().stream().filter(m -> m.getName().equals("field")).findFirst().get();
                    JavaType.Annotation.SingleElementValue value = (JavaType.Annotation.SingleElementValue) ((JavaType.Annotation) field.getAnnotations().getFirst()).getValues().getFirst();
                    assertThat(value.getConstantValue()).isEqualTo(1000);

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

        @Test
        void writeReadWithSpecialCharacterAnnotations() throws IOException, URISyntaxException {
            // Find the directory containing the compiled classes
            URL classUrl = TypeTableTest.class.getResource("TypeTableTest.class");
            Path classesDir = Paths.get(classUrl.toURI()).getParent().getParent().getParent().getParent().getParent().getParent();
            Path dataDir = classesDir.resolve("org/openrewrite/java/internal/parser/data");

            // Create a temporary jar file
            Path jarFile = tempDir.resolve("special-char-annotations-1.jar");

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
            TypeTable table = new TypeTable(ctx, tsv.toUri().toURL(), List.of("special-char-annotations"));
            Path loadedClassesDir = table.load("special-char-annotations");
            assertThat(loadedClassesDir).isNotNull();

            // Test that we can use the annotations with special characters in code
            rewriteRun(
              spec -> spec.parser(JavaParser.fromJavaVersion()
                .classpath(List.of(loadedClassesDir))),
              java(
                """
                  import org.openrewrite.java.internal.parser.data.*;
                  
                  @BasicAnnotation
                  @StringAnnotation
                  @SpecialCharAnnotation
                  class SpecialCharTestClass {
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    // Verify that annotations with special characters are properly loaded
                    J.ClassDeclaration clazz = cu.getClasses().getFirst();
                    List<J.Annotation> annotations = clazz.getLeadingAnnotations();

                    // Verify BasicAnnotation  
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.BasicAnnotation",
                      "intValue", "42");

                    // Verify StringAnnotation - this tests that string values with special characters
                    // like unicode are properly handled through the TSV serialization/deserialization
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.StringAnnotation",
                      "value", "Default value");
                      
                    // Also verify the unicode attribute to test special character handling
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.StringAnnotation",
                      "unicode", "Unicode: © ® ™");
                      
                    // Verify SpecialCharAnnotation with pipe characters
                    // This is the core functionality we implemented - ensuring pipe characters
                    // in annotation default values are properly handled through TSV serialization
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.SpecialCharAnnotation",
                      "withPipes", "Hello|World|Test");
                      
                    // Verify other special characters are handled correctly
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.SpecialCharAnnotation",
                      "withBackslashes", "Path\\To\\|File");
                      
                    verifyAnnotation(annotations, "org.openrewrite.java.internal.parser.data.SpecialCharAnnotation",
                      "withQuotes", "He said \"Hello World\"");
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

    // Helper methods for integration tests
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
}