/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ReplaceConstant;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ReplaceConstantTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceConstant("java.nio.charset.StandardCharsets", "UTF_8", "\"UTF-8\""));
    }

    @Test
    void replaceConstantInTrailingLambda() {
        rewriteRun(
          kotlin(
            """
              import java.nio.charset.StandardCharsets.UTF_8

              class Test {
                  fun supply(name: String, supplier: () -> Any) {}
                  fun test() {
                      supply("charset") { UTF_8 }
                  }
              }
              """,
            """
              class Test {
                  fun supply(name: String, supplier: () -> Any) {}
                  fun test() {
                      supply("charset") { "UTF-8" }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstantInMethodBody() {
        rewriteRun(
          kotlin(
            """
              import java.nio.charset.StandardCharsets.UTF_8

              class Test {
                  fun test(): Any {
                      return UTF_8
                  }
              }
              """,
            """
              class Test {
                  fun test(): Any {
                      return "UTF-8"
                  }
              }
              """
          )
        );
    }
}
