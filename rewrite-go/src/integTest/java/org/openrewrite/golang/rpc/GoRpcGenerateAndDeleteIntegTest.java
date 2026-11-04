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
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.rpc.request.BatchVisit;
import org.openrewrite.rpc.request.BatchVisitResponse;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.golang.Assertions.goMod;
import static org.openrewrite.golang.Assertions.goSum;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoRpcGenerateAndDeleteIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(Paths.get("build/rewrite-go-rpc").toAbsolutePath())
          .log(tempDir.resolve("go-rpc.log"))
          .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false)
          .typeValidationOptions(TypeValidation.none());
    }

    private static GoRewriteRpc client() {
        return GoRewriteRpc.getOrStart();
    }

    @Test
    void runScanningRecipeThatGeneratesGo() {
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe("org.openrewrite.golang.test.GenerateSourceFile",
            Map.of("sourcePath", "doc.go", "text", "// Package main does nothing.\npackage main\n"))),
          go(
            null,
            """
              // Package main does nothing.
              package main
              """,
            spec -> spec.path("doc.go")
              .afterRecipe(cu -> assertThat(cu).isInstanceOf(Go.CompilationUnit.class))
          )
        );
    }

    @Test
    void runScanningRecipeThatGeneratesGoMod() {
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe("org.openrewrite.golang.test.GenerateSourceFile",
            Map.of("sourcePath", "go.mod", "text", "module example.com/hello\n\ngo 1.22\n"))),
          goMod(
            null,
            """
              module example.com/hello

              go 1.22
              """,
            spec -> spec.afterRecipe(mod -> assertThat(mod).isInstanceOf(GoMod.class))
          )
        );
    }

    @Test
    void runRecipeThatDeletes() {
        rewriteRun(
          spec -> spec
            .recipe(client().prepareRecipe("org.openrewrite.golang.test.DeleteSourceFile",
              Map.of("sourcePath", "old.go")))
            .dataTable(SourcesFileResults.Row.class, rows -> assertThat(rows)
              .anySatisfy(row -> {
                  assertThat(row.getSourcePath()).isEqualTo("old.go");
                  assertThat(row.getAfterSourcePath()).isEmpty();
              })),
          go(
            """
              package main

              func old() {
              }
              """,
            null,
            spec -> spec.path("old.go")
          ),
          go(
            """
              package main

              func main() {
              }
              """,
            spec -> spec.path("main.go")
          )
        );
    }

    @Test
    void runRecipeThatDeletesGoSum() {
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe("org.openrewrite.golang.test.DeleteSourceFile",
            Map.of("sourcePath", "go.sum"))),
          goSum(
            """
              github.com/stretchr/testify v1.8.0 h1:aaaa=
              github.com/stretchr/testify v1.8.0/go.mod h1:bbbb=
              """,
            (String) null
          )
        );
    }

    @Test
    void runBatchedRecipesThatDelete() throws IOException {
        rewriteRun(
          spec -> spec
            .recipes(
              client().prepareRecipe("org.openrewrite.golang.test.DeleteSourceFile", Map.of("sourcePath", "old.go")),
              client().prepareRecipe("org.openrewrite.golang.test.RenameXToFlag")
            )
            .dataTable(SourcesFileResults.Row.class, rows -> assertThat(rows)
              .anySatisfy(row -> {
                  assertThat(row.getSourcePath()).isEqualTo("old.go");
                  assertThat(row.getAfterSourcePath()).isEmpty();
              })),
          go(
            """
              package main

              var x = 1
              """,
            null,
            spec -> spec.path("old.go")
          ),
          go(
            """
              package main

              func f() {
              \tvar x = true
              \t_ = x
              }
              """,
            """
              package main

              func f() {
              \tvar flag = true
              \t_ = flag
              }
              """,
            spec -> spec.path("main.go")
          )
        );
        assertThat(Files.readString(tempDir.resolve("go-rpc.log"))).contains("BatchVisit");
    }

    @Test
    void treeDeletedInBatchVisitIsNotServedAfterwards() {
        // given
        RpcRecipe delete = (RpcRecipe) client().prepareRecipe("org.openrewrite.golang.test.DeleteSourceFile",
          Map.of("sourcePath", "old.go"));
        Go.CompilationUnit old = GolangParser.builder().build()
          .parse("package main\n")
          .map(sf -> (Go.CompilationUnit) sf.withSourcePath(Path.of("old.go")))
          .findFirst()
          .orElseThrow();

        // when
        BatchVisitResponse response = client().batchVisit(old, new InMemoryExecutionContext(), null,
          List.of(new BatchVisit.BatchVisitItem(delete.getEditVisitor(), null)));

        // then
        assertThat(response.getResults())
          .singleElement()
          .satisfies(r -> assertThat(r.isDeleted()).isTrue());
        assertThat((Object) client().getObject(old.getId().toString(), Go.CompilationUnit.class.getName())).isNull();
    }

    @Test
    void runScanningRecipeThatGeneratesFromScannedFileAndDeletesIt() {
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe("org.openrewrite.golang.test.MoveSourceFile",
            Map.of("from", "old.go", "to", "new.go"))),
          go(
            """
              package main

              func hello() {
              }
              """,
            null,
            spec -> spec.path("old.go")
          ),
          go(
            null,
            """
              package main

              func hello() {
              }
              """,
            spec -> spec.path("new.go")
              .afterRecipe(cu -> assertThat(cu).isInstanceOf(Go.CompilationUnit.class))
          )
        );
    }

    @Test
    void runScanningRecipeThatMovesGoMod() {
        rewriteRun(
          spec -> spec.recipe(client().prepareRecipe("org.openrewrite.golang.test.MoveSourceFile",
            Map.of("from", "go.mod", "to", "sub/go.mod"))),
          goMod(
            """
              module example.com/hello

              go 1.22
              """,
            (String) null
          ),
          goMod(
            null,
            """
              module example.com/hello

              go 1.22
              """,
            spec -> spec.path("sub/go.mod")
          )
        );
    }
}
