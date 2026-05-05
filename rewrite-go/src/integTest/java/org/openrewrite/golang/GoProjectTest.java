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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.golang.Assertions.goMod;
import static org.openrewrite.golang.Assertions.goProject;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoProjectTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(binaryPath)
          .log(tempDir.resolve("go-rpc.log"))
          .traceRpcMessages());
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

    /**
     * Direct test of the project-aware Parse RPC: a parser configured with
     * module + go.mod content sends the project context to the Go server,
     * which builds a ProjectImporter so the parsed Go.CompilationUnit gets
     * type attribution on third-party imports declared in `require`.
     */
    @Test
    void thirdPartyImportResolvesViaModuleContext() {
        String goModContent =
                "module example.com/foo\n\n" +
                "go 1.22\n\n" +
                "require github.com/x/y v1.2.3\n";

        GolangParser parser = GolangParser.builder()
                .module("example.com/foo")
                .goMod(goModContent)
                .build();

        SourceFile sf = parser.parse(
                "package main\n\n" +
                "import \"github.com/x/y\"\n\n" +
                "func main() { _ = y.Hello() }\n"
        ).findFirst().orElseThrow();

        assertThat(sf).isInstanceOf(Go.CompilationUnit.class);
        // Walk the parsed CU for the `y` package-alias identifier and
        // confirm its type came back resolved. Without module context it
        // would be null because importer.Default() doesn't know about
        // github.com/x/y.
        boolean[] sawResolvedY = {false};
        new org.openrewrite.java.JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                if ("y".equals(ident.getSimpleName()) && ident.getType() != null) {
                    sawResolvedY[0] = true;
                }
                return super.visitIdentifier(ident, p);
            }
        }.visit(sf, 0);
        assertThat(sawResolvedY[0])
                .as("expected `y` import identifier to have a non-nil Type via project context")
                .isTrue();
    }

    @Test
    void goModAndGoFilesAreSiblingsTaggedWithProjectMarker() {
        rewriteRun(
          goProject("foo",
            goMod(
              """
                module example.com/foo

                go 1.22

                require github.com/x/y v1.2.3
                """,
              s -> s.afterRecipe(pt -> {
                  // The goMod source carries the resolution result.
                  GoResolutionResult mrr = pt.getMarkers().findFirst(GoResolutionResult.class).orElseThrow();
                  assertThat(mrr.getModulePath()).isEqualTo("example.com/foo");
                  assertThat(mrr.getGoVersion()).isEqualTo("1.22");
                  // And the project marker added by goProject(...).
                  GoProject project = pt.getMarkers().findFirst(GoProject.class).orElseThrow();
                  assertThat(project.getProjectName()).isEqualTo("foo");
              })),
            go(
              """
                package main

                import "github.com/x/y"

                func main() { y.Hello() }
                """,
              s -> s.afterRecipe(cu -> {
                  // The .go file carries GoProject (from the wrapper) but NOT
                  // GoResolutionResult — that lives on the sibling go.mod, just
                  // as MavenResolutionResult lives on the sibling pom.xml.
                  GoProject project = cu.getMarkers().findFirst(GoProject.class).orElseThrow();
                  assertThat(project.getProjectName()).isEqualTo("foo");
                  assertThat(cu.getMarkers().findFirst(GoResolutionResult.class)).isEmpty();
              }))
          )
        );
    }
}
