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
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    void dependenciesFromResources(@TempDir Path temp) {
        JavaParserExecutionContextView ctx = JavaParserExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setParserClasspathDownloadTarget(temp.toFile());
        assertThat(JavaParser.dependenciesFromResources(ctx, "guava-31.0-jre")).isNotEmpty();
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
            spec -> spec.afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Path.of("my","example","PublicClass.java")))
          )
        );
    }
}
