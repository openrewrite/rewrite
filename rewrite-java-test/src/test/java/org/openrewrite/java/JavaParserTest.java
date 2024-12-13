/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openrewrite.java.Assertions.java;

/**
 * @author Alex Boyko
 */
class JavaParserTest implements RewriteTest {

    @Test
    void incompleteAssignment() {
        rewriteRun(
          java(
            """
              @Deprecated(since=)
              public class A {}
              """
          )
        );
    }

    @SuppressWarnings("RedundantSuppression")
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    @Test
    void annotationCommentWithNoSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("serial")// fred
              @Deprecated
              public class PersistenceManagerImpl {
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(cu.getClasses().get(0).getLeadingAnnotations()).hasSize(2))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    @Test
    void annotationCommentWithSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("ALL") // fred
              @Deprecated
              public class PersistenceManagerImpl {
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(cu.getClasses().get(0).getLeadingAnnotations()).hasSize(2))
          )
        );
    }

    @Test
    void dependenciesFromResources(@TempDir Path temp) throws Exception {
        JavaParserExecutionContextView ctx = JavaParserExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setParserClasspathDownloadTarget(temp.toFile());
        // Put a decoy file in the target directory to ensure that it is not used
        Files.write(temp.resolve("guava-30.0-jre.jar"), "decoy for test purposes; not a real jar".getBytes());
        List<Path> classpath = JavaParser.dependenciesFromResources(ctx, "guava");
        assertThat(classpath)
          .singleElement()
          .matches(Files::exists, "File extracted from classpath resources exists on disk")
          .matches(path -> path.endsWith("guava-31.0-jre.jar"),
            "classpathFromResources should return guava-31.0-jre.jar from resources, even when the target " +
            "directory contains guava-30.0-jre.jar which has the same prefix");
    }

    @Test
    void getParserClasspathDownloadCreateRequiredFolder(@TempDir Path temp) throws Exception {
        Path updatedTemp = Path.of(temp.toString(), "someFolder");
        assertThat(updatedTemp.toFile().exists()).isFalse();
        JavaParserExecutionContextView ctx = JavaParserExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setParserClasspathDownloadTarget(updatedTemp.toFile());
        ctx.getParserClasspathDownloadTarget();
        assertThat(updatedTemp.toFile().exists()).isTrue();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3222")
    void parseFromByteArray() {
        try (ScanResult scan = new ClassGraph().scan()) {
            byte[][] classes = scan.getResourcesMatchingWildcard("javaparser-byte-array-tests/**.class").stream()
              .map(it -> {
                  try {
                      return it.read().array();
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
              })
              .toArray(byte[][]::new);

            JavaParser parser = JavaParser.fromJavaVersion()
              .classpath(classes)
              .build();

            @Language("java")
            String source = """
              import example.InterfaceA;
              public class User implements InterfaceA, InterfaceB {
                @Override
                public void methodA() {}
              
                @Override
               public void methodB() {}
              }
              """;
            Stream<SourceFile> compilationUnits = parser.parse(new InMemoryExecutionContext(Throwable::printStackTrace), source);
            assertThat(compilationUnits.map(J.CompilationUnit.class::cast)).singleElement()
              .satisfies(cu -> assertThat(cu.getClasses()).singleElement()
                .satisfies(cd -> assertThat(cd.getImplements()).satisfiesExactly(
                  i -> assertThat(i.getType()).hasToString("example.InterfaceA"),
                  i -> assertThat(i.getType()).hasToString("InterfaceB")
                )));
        }
    }

    @ParameterizedTest
    // language=java
    @ValueSource(strings = {
      "package my.example; class PrivateClass { void foo() {} } public class PublicClass { void bar() {} }",
      "package my.example; public class PublicClass { void bar() {} } class PrivateClass { void foo() {} }"
    })
    void shouldResolvePathUsingPublicClasses(@Language("java") String source) {
        rewriteRun(
          java(
            source,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Path.of("my", "example", "PublicClass.java")))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1895")
    void moduleInfo() {
        // Ignored until properly handled: https://github.com/openrewrite/rewrite/issues/4054#issuecomment-2267605739
        assertFalse(JavaParser.fromJavaVersion().build().accept(Path.of("src/main/java/foo/module-info.java")));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/4624")
    void shouldParseComments() {
        rewriteRun(
          java(
            """
              class A {
                  /*
                   * public Some getOther() { return other; }
                   *
                   *//**
                   * Sets the value of the other property.
                   *
                   * @param value allowed object is {@link Some }
                   *
                   *//*
                   * public void setOther(Some value) { this.other =
                   * value; }
                   */
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getClasses().get(0).getBody().getEnd().getComments())
              .extracting("text")
              .containsExactly(
                """
                  
                       * public Some getOther() { return other; }
                       *
                       \
                  """,
                """
                  *
                       * Sets the value of the other property.
                       *
                       * @param value allowed object is {@link Some }
                       *
                       \
                  """,
                """
                  
                       * public void setOther(Some value) { this.other =
                       * value; }
                       \
                  """
              ))
          )
        );
    }

    @Test
    void filterArtifacts() {
        List<URI> classpath = List.of(
          URI.create("file:/.m2/repository/com/google/guava/guava-24.1.1/com_google_guava_guava-24.1.1.jar"),
          URI.create("file:/.m2/repository/org/threeten/threeten-extra-1.5.0/org_threeten_threeten_extra-1.5.0.jar"),
          URI.create("file:/.m2/repository/com/amazonaws/aws-java-sdk-s3-1.11.546/com_amazonaws_aws_java_sdk_s3-1.11.546.jar"),
          URI.create("file:/.m2/repository/org/openrewrite/rewrite-java/8.41.1/rewrite-java-8.41.1.jar")
        );
        assertThat(JavaParser.filterArtifacts("threeten-extra", classpath))
          .containsOnly(Paths.get("/.m2/repository/org/threeten/threeten-extra-1.5.0/org_threeten_threeten_extra-1.5.0.jar"));
        assertThat(JavaParser.filterArtifacts("guava", classpath))
          .containsOnly(Paths.get("/.m2/repository/com/google/guava/guava-24.1.1/com_google_guava_guava-24.1.1.jar"));
        assertThat(JavaParser.filterArtifacts("aws-java-sdk-s3", classpath))
          .containsOnly(Paths.get("/.m2/repository/com/amazonaws/aws-java-sdk-s3-1.11.546/com_amazonaws_aws_java_sdk_s3-1.11.546.jar"));
        assertThat(JavaParser.filterArtifacts("rewrite-java", classpath))
          .containsOnly(Paths.get("/.m2/repository/org/openrewrite/rewrite-java/8.41.1/rewrite-java-8.41.1.jar"));
    }
}
