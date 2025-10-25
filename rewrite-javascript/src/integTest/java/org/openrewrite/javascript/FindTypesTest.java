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
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.javascript.Assertions.*;

@SuppressWarnings({"TypeScriptCheckImport", "JSUnusedLocalSymbols"})
class FindTypesTest implements RewriteTest {
    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void findTypes(@TempDir Path projectDir) {
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
                import { /*~~>*/ClipLoader } from 'react-spinners';
                
                const App = () => {
                  return </*~~>*/ClipLoader color="#36d7b7" />;
                };
                """
            ),
            packageJson(
              """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "react": "^16.8.0",
                    "react-spinners": "^0.5.0"
                  },
                  "devDependencies": {
                    "@types/react": "^16.8.0"
                  }
                }
                """
            )
          )
        );
    }
}
