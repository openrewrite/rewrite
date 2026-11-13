/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.openrewrite.scala.Assertions.scala;

/**
 * A file holding nothing but whitespace and comments has no statements: the comments belong to the
 * compilation unit's EOF space, never to a {@link J.Unknown} covering the whole source.
 */
class CommentOnlyFileTest implements RewriteTest {

    /**
     * A null path exercises {@link ScalaParser#sourcePathFromSourceText}, which derives a {@code .sbt}
     * path for sources that declare neither a package nor a class-like declaration.
     */
    static Stream<Arguments> commentOnlySources() {
        return Stream.of(
          arguments(null, "// only a comment\n", 1),
          arguments("Foo.scala", "// only a comment\n", 1),
          arguments("build.sbt", "// only a comment\n", 1),
          arguments("Foo.scala", "// only a comment", 1),
          arguments("build.sbt", "// only a comment", 1),
          arguments("Foo.scala", "/* only a comment */\n", 1),
          arguments("Foo.scala", "/* only a comment */\n\n", 1),
          arguments("Foo.scala", "// first\n\n/* second\n * continued\n */\n// third\n", 3),
          arguments("Foo.scala", "\n\n", 0),
          arguments("build.sbt", "   \n", 0),
          arguments("Foo.scala", "", 0)
        );
    }

    @MethodSource("commentOnlySources")
    @ParameterizedTest
    void commentOnlyFile(@Nullable String path, @Language("scala") String source, int comments) {
        rewriteRun(
          scala(source, spec -> {
              if (path != null) {
                  spec.path(path);
              }
              spec.noTrim().afterRecipe(cu -> {
                  assertThat(cu.getStatements()).isEmpty();
                  assertThat(cu.getPackageDeclaration()).isNull();
                  assertThat(cu.getEof().getComments()).hasSize(comments);
              });
          })
        );
    }

    static Stream<Arguments> commentsAroundDeclarations() {
        return Stream.of(
          arguments("Foo.scala", "// leading comment\nclass A\n", 0),
          arguments("build.sbt", "// leading comment\nname := \"demo\"\n", 0),
          arguments("Foo.scala", "class A\n// trailing comment\n", 1)
        );
    }

    @MethodSource("commentsAroundDeclarations")
    @ParameterizedTest
    void commentAlongsideDeclaration(String path, @Language("scala") String source, int eofComments) {
        rewriteRun(
          scala(source, spec -> spec.path(path).noTrim().afterRecipe(cu -> {
              assertThat(cu.getStatements()).hasSize(1);
              assertThat(cu.getEof().getComments()).hasSize(eofComments);
          }))
        );
    }

    static Stream<Arguments> packageDeclarationsFollowedByComments() {
        return Stream.of(
          arguments("foo/Foo.scala", "package foo\n// trailing comment\n", 1),
          arguments("foo/Foo.scala", "package foo\n\n/* trailing comment */\n", 1),
          arguments("foo/bar/Foo.scala", "package foo.bar\n// first\n// second\n", 2),
          arguments("foo/Foo.scala", "package foo\n// no trailing newline", 1),
          arguments("foo/Foo.scala", "package foo\n\n", 0)
        );
    }

    @MethodSource("packageDeclarationsFollowedByComments")
    @ParameterizedTest
    void packageDeclarationFollowedByComments(String path, @Language("scala") String source, int eofComments) {
        rewriteRun(
          scala(source, spec -> spec.path(path).noTrim().afterRecipe(cu -> {
              assertThat(cu.getPackageDeclaration()).isNotNull();
              assertThat(cu.getStatements()).isEmpty();
              assertThat(cu.getEof().getComments()).hasSize(eofComments);
          }))
        );
    }
}
