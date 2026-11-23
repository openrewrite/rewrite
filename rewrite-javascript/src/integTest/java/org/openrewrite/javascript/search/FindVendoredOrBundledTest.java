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
package org.openrewrite.javascript.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.JavaScriptParser;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.javascript;
import static org.openrewrite.javascript.Assertions.typescript;

@SuppressWarnings("JSUnusedLocalSymbols")
class FindVendoredOrBundledTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindVendoredOrBundled());
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @DocumentExample
    @Test
    void vendoredDirectory() {
        rewriteRun(
          javascript(
            "const a = 1;",
            "/*~~>*/const a = 1;",
            spec -> spec.path("vendor/jquery.js")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "src/vendor/lib.js",
      "node_modules/lodash/index.js",
      "bower_components/angular/angular.js",
      "dist/index.js",
      "packages/core/dist/index.mjs",
      "public/app.bundle.js",
      "static/vendor.bundle.cjs"
    })
    void vendoredOrBundledPath(String path) {
        rewriteRun(
          javascript(
            "const a = 1;",
            "/*~~>*/const a = 1;",
            spec -> spec.path(path)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "index.js",
      "src/index.js",
      "build/webpack.config.js",
      "src/vendors.js",
      "src/distance.js",
      "src/minify.js",
      "src/bundle.js"
    })
    void ownSourcePath(String path) {
        rewriteRun(
          javascript(
            "const a = 1;",
            spec -> spec.path(path)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"public/app.min.js", "lib/index.min.mjs"})
    void minifiedPathParsedAsCompilationUnit(String path) {
        // given
        SourceFile cu = JavaScriptParser.builder().build()
          .parseInputs(List.of(Parser.Input.fromString(Path.of("a.js"), "const a = 1;")), null, new InMemoryExecutionContext())
          .findFirst()
          .orElseThrow()
          .withSourcePath(Path.of(path));

        // when
        SourceFile after = (SourceFile) new FindVendoredOrBundled().getVisitor().visit(cu, new InMemoryExecutionContext());

        // then
        assertThat(after).isNotNull();
        assertThat(after.getMarkers().findFirst(SearchResult.class)).isPresent();
    }

    @Test
    void typeScriptInVendoredDirectory() {
        rewriteRun(
          typescript(
            "const a: number = 1;",
            "/*~~>*/const a: number = 1;",
            spec -> spec.path("vendor/lib.ts")
          )
        );
    }

    @Test
    void trailingSourceMappingUrl() {
        rewriteRun(
          javascript(
            """
              const a = 1;
              //# sourceMappingURL=index.js.map
              """,
            """
              /*~~>*/const a = 1;
              //# sourceMappingURL=index.js.map
              """,
            spec -> spec.path("lib/index.js")
          )
        );
    }

    @Test
    void legacyTrailingSourceMappingUrl() {
        rewriteRun(
          javascript(
            """
              const a = 1;
              //@ sourceMappingURL=index.js.map
              """,
            """
              /*~~>*/const a = 1;
              //@ sourceMappingURL=index.js.map
              """,
            spec -> spec.path("lib/index.js")
          )
        );
    }

    @Test
    void sourceMappingUrlNotTrailing() {
        rewriteRun(
          javascript(
            """
              //# sourceMappingURL=index.js.map
              const a = 1;
              """,
            spec -> spec.path("lib/index.js")
          )
        );
    }
}
