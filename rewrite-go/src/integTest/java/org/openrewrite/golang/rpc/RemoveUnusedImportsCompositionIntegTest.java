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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;

/**
 * A reference an earlier recipe introduced has to still read as a use of its import
 * when a later recipe runs, and both recipes reach the tree over RPC.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RemoveUnusedImportsCompositionIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(binaryPath)
          .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
          .allowNonWhitespaceInWhitespace(true)
          .identifiers(false)
          .methodInvocations(false)
          .build());
    }

    /** One scheduler cycle, which is what batches both recipes into a single RPC. */
    @Test
    void importAddedByAnEarlierRecipeSurvivesTheComposite() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        rewriteRun(
          spec -> spec.recipes(
            rpc.prepareRecipe("org.openrewrite.golang.test.WrapErrorWithContext"),
            rpc.prepareRecipe("org.openrewrite.golang.RemoveUnusedImports")),
          go(
            """
              package main

              import (
              \t"errors"
              )

              func doWork() error {
              \tif err := errors.New("boom"); err != nil {
              \t\treturn err
              \t}
              \treturn nil
              }
              """,
            """
              package main

              import (
              \t"errors"
              \t"fmt"
              )

              func doWork() error {
              \tif err := errors.New("boom"); err != nil {
              \t\treturn fmt.Errorf("doWork: %w", err)
              \t}
              \treturn nil
              }
              """
          )
        );
    }

    @Test
    void importAddedByAnEarlierRecipeSurvives() {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        String source = """
          package main

          import (
          \t"errors"
          )

          func doWork() error {
          \tif err := errors.New("boom"); err != nil {
          \t\treturn err
          \t}
          \treturn nil
          }
          """;
        SourceFile cu = GolangParser.builder().build().parse(source).findFirst().orElseThrow();
        rpc.reset();

        var ctx = new InMemoryExecutionContext();
        Tree wrapped = rpc.prepareRecipe("org.openrewrite.golang.test.WrapErrorWithContext")
          .getVisitor().visit(cu, ctx);
        assertThat(rpc.print((SourceFile) wrapped))
          .contains("fmt.Errorf(\"doWork: %w\", err)")
          .contains("\"fmt\"");

        Tree pruned = rpc.prepareRecipe("org.openrewrite.golang.RemoveUnusedImports")
          .getVisitor().visit(wrapped, ctx);
        assertThat(rpc.print((SourceFile) pruned))
          .contains("fmt.Errorf(\"doWork: %w\", err)")
          .contains("\"fmt\"");
    }
}
