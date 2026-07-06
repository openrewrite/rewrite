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
package org.openrewrite.golang.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.golang.RegenerateGoSum;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.openrewrite.test.SourceSpecs.text;

class LockFileRegenerationTest implements RewriteTest {

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void moduleWithoutDependenciesProducesEmptyGoSum() {
        // given
        assumeThat(GoExecutor.GO.find()).isNotNull();
        String goMod = "module example.com/foo\n\ngo 1.21\n";

        // when
        LockFileRegeneration.Result result = LockFileRegeneration.GO_SUM.regenerate(goMod);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).isEmpty();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void invalidGoModFailsGracefully() {
        // given
        assumeThat(GoExecutor.GO.find()).isNotNull();
        String goMod = "this is not a valid go.mod\n";

        // when
        LockFileRegeneration.Result result = LockFileRegeneration.GO_SUM.regenerate(goMod);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void populatesGoSumFromGoMod() {
        // given
        assumeThat(GoExecutor.GO.find()).isNotNull();

        // when / then
        rewriteRun(
          spec -> spec.recipe(new RegenerateGoSum()),
          text(
            """
            module example.com/foo

            go 1.21

            require rsc.io/quote v1.5.2
            """,
            spec -> spec.path("go.mod")
          ),
          text(
            "",
            """
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c h1:qgOY6WgZOaTkIIMiVjBQcw93ERBE4m30iBm00nkL0i8=
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c/go.mod h1:NqM8EUOU14njkJ3fqMW+pc6Ldnwhi/IjpwHt7yyuwOQ=
            rsc.io/quote v1.5.2 h1:w5fcysjrx7yqtD/aO+QwRjYZOKnaM9Uh2b40tElTs3Y=
            rsc.io/quote v1.5.2/go.mod h1:LzX7hefJvL54yjefDEDHNONDjII0t9xZLPXsUe+TKr0=
            rsc.io/sampler v1.3.0 h1:7uVkIFmeBqHfdjD+gZwtXXI+RODJ2Wc4O7MPEh/QiW4=
            rsc.io/sampler v1.3.0/go.mod h1:T1hPZKmBbMNahiBKFy5HrXp6adAjACjK9JXDnKaTXpA=
            """,
            spec -> spec.path("go.sum").noTrim()
          )
        );
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void prunesStaleEntriesAndAddsMissing() {
        // given
        assumeThat(GoExecutor.GO.find()).isNotNull();

        // when / then
        rewriteRun(
          spec -> spec.recipe(new RegenerateGoSum()),
          text(
            """
            module example.com/foo

            go 1.21

            require rsc.io/quote v1.5.2
            """,
            spec -> spec.path("go.mod")
          ),
          text(
            """
            example.com/stale v9.9.9 h1:deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdea=
            example.com/stale v9.9.9/go.mod h1:deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdea=
            """,
            """
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c h1:qgOY6WgZOaTkIIMiVjBQcw93ERBE4m30iBm00nkL0i8=
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c/go.mod h1:NqM8EUOU14njkJ3fqMW+pc6Ldnwhi/IjpwHt7yyuwOQ=
            rsc.io/quote v1.5.2 h1:w5fcysjrx7yqtD/aO+QwRjYZOKnaM9Uh2b40tElTs3Y=
            rsc.io/quote v1.5.2/go.mod h1:LzX7hefJvL54yjefDEDHNONDjII0t9xZLPXsUe+TKr0=
            rsc.io/sampler v1.3.0 h1:7uVkIFmeBqHfdjD+gZwtXXI+RODJ2Wc4O7MPEh/QiW4=
            rsc.io/sampler v1.3.0/go.mod h1:T1hPZKmBbMNahiBKFy5HrXp6adAjACjK9JXDnKaTXpA=
            """,
            spec -> spec.path("go.sum").noTrim()
          )
        );
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void createsGoSumWhenAbsent() {
        // given
        assumeThat(GoExecutor.GO.find()).isNotNull();

        // when / then
        rewriteRun(
          spec -> spec.recipe(new RegenerateGoSum()),
          text(
            """
            module example.com/foo

            go 1.21

            require rsc.io/quote v1.5.2
            """,
            spec -> spec.path("go.mod")
          ),
          text(
            null,
            """
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c h1:qgOY6WgZOaTkIIMiVjBQcw93ERBE4m30iBm00nkL0i8=
            golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c/go.mod h1:NqM8EUOU14njkJ3fqMW+pc6Ldnwhi/IjpwHt7yyuwOQ=
            rsc.io/quote v1.5.2 h1:w5fcysjrx7yqtD/aO+QwRjYZOKnaM9Uh2b40tElTs3Y=
            rsc.io/quote v1.5.2/go.mod h1:LzX7hefJvL54yjefDEDHNONDjII0t9xZLPXsUe+TKr0=
            rsc.io/sampler v1.3.0 h1:7uVkIFmeBqHfdjD+gZwtXXI+RODJ2Wc4O7MPEh/QiW4=
            rsc.io/sampler v1.3.0/go.mod h1:T1hPZKmBbMNahiBKFy5HrXp6adAjACjK9JXDnKaTXpA=
            """,
            spec -> spec.path("go.sum").noTrim()
          )
        );
    }
}
