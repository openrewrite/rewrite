/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("All")
class RemoveTrailingSemicolonTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveTrailingSemicolon());
    }

    @DocumentExample
    @Test
    void variableDeclaration() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  var foo = 1;
                  var bar: Int;
                  var baz: String = "a";
              }
              """,
            """
              fun method() {
                  var foo = 1
                  var bar: Int
                  var baz: String = "a"
              }
              """
          )
        );
    }

    @Test
    void doNotChangeVariableDeclarationsInSameLine() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  var foo = 1; var bar: Int
              }
              """
          )
        );
    }

    @Test
    void methodInvocation() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method (t : Test): Test {
                      method(this);
                      method(t);
                      method(method(t));
                      return this
                  }
              }
              """,
            """
              class Test {
                  fun method (t : Test): Test {
                      method(this)
                      method(t)
                      method(method(t))
                      return this
                  }
              }
              """
          )
        );
    }

    @Test
    void operators() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method() {
                      var i = 0;
                      i++;
                      --i;
                      i++; ++i;
                  };
              }
              """,
            """
              class Test {
                  fun method() {
                      var i = 0
                      i++
                      --i
                      i++; ++i
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeIfMethodInvocationsAreInASameLine() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method (t : Test): Test {
                      method(this); method(t)
                      return this
                  }
              }
              """
          )
        );
    }

    @Test
    void imports() {
        rewriteRun(
          kotlin(
            """
              import java.util.List;
              import java.util.Map;

              class T
              """,
            """
              import java.util.List
              import java.util.Map

              class T
              """
          )
        );
    }

    @Test
    void noSemicolonAfterReturn() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method(): Int {
                      return 1;
                  }
              }
              """,
            """
              class Test {
                  fun method(): Int {
                      return 1
                  }
              }
              """
          )
        );
    }

    @Test
    void noSemicolonAfterReturn2() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method(): Int {
                      return if (true) 1 else 2;
                  }
              }
              """,
            """
              class Test {
                  fun method(): Int {
                      return if (true) 1 else 2
                  }
              }
              """
          )
        );
    }

    @Test
    void ifStatement() {
        rewriteRun(
          kotlin(
            """
              fun method () {
                  if (true) {
                  };
              }
              """,
            """
              fun method () {
                  if (true) {
                  }
              }
              """
          )
        );
    }
}
