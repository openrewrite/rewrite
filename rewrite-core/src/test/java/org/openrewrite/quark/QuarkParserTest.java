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
package org.openrewrite.quark;

import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class QuarkParserTest implements RewriteTest {

    @Test
    void allOthers() {
        rewriteRun(
          spec -> spec.beforeRecipe(sources -> {
              try {
                  List<SourceFile> quarks = QuarkParser.parseAllOtherFiles(Paths.get("../"), sources).toList();
                  assertThat(quarks).isNotEmpty();
                  assertThat(quarks.stream().map(SourceFile::getSourcePath))
                    .doesNotContain(Paths.get("build.gradle.kts"));
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }

          }),
          text("hi", spec -> spec.path(Paths.get("build.gradle.kts")))
        );
    }

    @Test
    void oneQuark() {
        rewriteRun(
          spec -> spec.beforeRecipe(sources -> assertThat(sources.stream().filter(s -> s instanceof Quark)).hasSize(1)),
          text("hi"),
          other("jon")
        );
    }

    @Test
    void renameQuark(@TempDir Path tempDir) {
        rewriteRun(
          spec ->
            spec
              .expectedCyclesThatMakeChanges(1)
              .recipe(toRecipe(() -> new TreeVisitor<>() {
                  @Override
                  public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                      SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                      if (sourceFile.getSourcePath().toString().endsWith(".bak")) {
                          return sourceFile;
                      }
                      return sourceFile.withSourcePath(Paths.get(sourceFile.getSourcePath() + ".bak"));
                  }
              }))
              .afterRecipe(run -> {
                  try {
                      for (Result result : run.getChangeset().getAllResults()) {
                          try (var git = Git.init().setDirectory(tempDir.toFile()).call()) {
                              git.apply().setPatch(new ByteArrayInputStream(result.diff().getBytes())).call();
                          }
                      }
                      assertThat(tempDir.toFile().list())
                        .containsExactlyInAnyOrder("hi.txt.bak", "jon.bak", ".git");
                      assertThat(Files.readString(tempDir.resolve("jon.bak")).trim()).isEqualTo("jon");
                  } catch (IOException | GitAPIException e) {
                      fail(e);
                  }
              }),
          text(
            "hi",
            spec -> spec
              .path("hi.txt")
              .beforeRecipe(s -> Files.writeString(tempDir.resolve(s.getSourcePath()), "hi"))
              .afterRecipe(s -> assertThat(s.getSourcePath()).isEqualTo(Paths.get("hi.txt.bak")))
          ),
          other(
            "jon",
            spec -> spec
              .path("jon")
              .beforeRecipe(s -> Files.writeString(tempDir.resolve(s.getSourcePath()), "jon"))
              .afterRecipe(s -> assertThat(s.getSourcePath()).isEqualTo(Paths.get("jon.bak")))
          )
        );
    }
}
