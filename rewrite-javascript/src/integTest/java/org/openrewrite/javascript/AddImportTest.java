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
package org.openrewrite.javascript;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.java.AddImport;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.typescript;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Timeout(60)
class AddImportTest implements RewriteTest {

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void javaImportNotAddedToTypeScript() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.jspecify.annotations.Nullable", null, false))),
          typescript(
            """
              import {IToken} from "./token";

              export class A {
                  public find(): IToken | null {
                      return null;
                  }
              }
              """
          )
        );
    }
}
