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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.tree.JSX;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.*;

@SuppressWarnings({"TypeScriptCheckImport", "JSUnusedLocalSymbols"})
public class FindTypesTest implements RewriteTest {
    //    @TempDir Path projectDir;
    Path projectDir = Paths.get(".working-dir");

    @BeforeEach
    void before() {
        if (!Files.exists(projectDir)) {
            assertThat(projectDir.toFile().mkdirs()).isTrue();
        }
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void findTypes() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("React.Component", true)),
          npm(
            projectDir,
            tsx(
              """
                import { ClipLoader } from 'react-spinners';
                
                const App = () => {
                  return <ClipLoader color="#36d7b7" />;
                };
                """,
              """
                import { ClipLoader } from 'react-spinners';
                
                const App = () => {
                  return </*~~>*/ClipLoader color="#36d7b7" />;
                };
                """,
              spec -> spec.beforeRecipe(cu -> {
                  new JavaScriptIsoVisitor<Integer>() {
                      @Override
                      public JSX.Tag visitJsxTag(JSX.Tag tag, Integer p) {
                          return super.visitJsxTag(tag, p);
                      }
                  }.visit(cu, 0);
              })
            ),
            packageJson(
              """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "react-spinners": "^0.13.8"
                  }
                }
                """
            )
          )
        );
    }
}
