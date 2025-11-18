/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.*;

@SuppressWarnings({"TypeScriptCheckImport", "JSUnusedLocalSymbols"})
class FindMethodsTest implements RewriteTest {
    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void findMethods(@TempDir Path projectDir) {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("_.LoDashStatic max(..)", false))
            .dataTable(MethodCalls.Row.class, rows ->
              assertThat(rows.stream().map(MethodCalls.Row::getMethod)).containsExactly("_.max(1, 2)")),
          npm(
            projectDir,
            typescript(
              """
                import _ from 'lodash';
                const result = _.max(1, 2);
                """,
              """
                import _ from 'lodash';
                const result = /*~~>*/_.max(1, 2);
                """
            ),
            packageJson(
              """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  },
                  "devDependencies": {
                    "@types/lodash": "^4.17.20"
                  }
                }
                """
            )
          )
        );
    }

    @Test
    void split() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("*..* split(..)", false)),
          typescript(
            "'hello world'.split(' ')",
            "/*~~>*/'hello world'.split(' ')"
          )
        );
    }

    @Test
    void splitWithTemplateString() {
//        JavaScriptRewriteRpc.getOrStart().traceGetObject(false, true);
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("*..* split(..)", false)),
          javascript(
            "'hello'.split(`; ${cookieName}=`)",
            "/*~~>*/'hello'.split(`; ${cookieName}=`)"
          )
        );
    }

    @Test
    void insideRightPaddedStatement() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("node *(..)", false))
            .dataTable(MethodCalls.Row.class, rows ->
              assertThat(rows.stream().map(MethodCalls.Row::getMethod)).containsExactly("assert(path, 'hello world')")),
          typescript(
            """
              import assert from 'node:assert'
              
              const publishNxProject = (path: string): void => {
                assert(path, 'hello world')
              }
              """,
            """
              import assert from 'node:assert'
              
              const publishNxProject = (path: string): void => {
                /*~~>*/assert(path, 'hello world')
              }
              """
          )
        );
    }

    @Test
    void nodeBuiltinModules() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("node assert(..)", false)),
          typescript(
            """
              import assert from 'node:assert';
              assert('hello', 'world');
              """,
            """
              import assert from 'node:assert';
              /*~~>*/assert('hello', 'world');
              """
          )
        );
    }

    @Test
    void defaultExportFunctions(@TempDir Path projectDir) {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("express <default>(..)", false))
            .dataTable(MethodCalls.Row.class, rows ->
              assertThat(rows.stream().map(MethodCalls.Row::getMethod)).containsExactly("express()")),
          npm(
            projectDir,
            typescript(
              """
                import express from 'express';
                const app = express();
                """,
              """
                import express from 'express';
                const app = /*~~>*/express();
                """
            ),
            packageJson(
              """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "express": "^4.18.2"
                  },
                  "devDependencies": {
                    "@types/express": "^4.17.21"
                  }
                }
                """
            )
          )
        );
    }
}
