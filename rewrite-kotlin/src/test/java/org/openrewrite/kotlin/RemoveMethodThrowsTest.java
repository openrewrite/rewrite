/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.RemoveMethodThrows;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RemoveMethodThrowsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveMethodThrows("* foo(..)", true, "java.io.IOException"));
    }

    @Test
    void classWrappedFunction() {
        rewriteRun(
          kotlin(
            """
              class Foo {
                  fun foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void topLevelFunctionDoesNotCrash() {
        rewriteRun(
          kotlin(
            """
              fun foo() {
              }
              """
          )
        );
    }

    @Test
    void topLevelExtensionFunction() {
        rewriteRun(
          spec -> spec.recipe(new RemoveMethodThrows("* toBar(..)", true, "java.io.IOException")),
          kotlin(
            """
              class Foo
              class Bar(val x: Int)

              fun Foo.toBar() = Bar(1)
              """
          )
        );
    }

    @Test
    void removeSoleExceptionFromThrowsAnnotation() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              class Foo {
                  @Throws(IOException::class)
                  fun foo() {
                  }
              }
              """,
            """
              class Foo {
                  fun foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeOneExceptionFromMultiThrowsAnnotation() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              class Foo {
                  @Throws(IOException::class, RuntimeException::class)
                  fun foo() {
                  }
              }
              """,
            """
              class Foo {
                  @Throws(RuntimeException::class)
                  fun foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeThrowsAnnotationFromTopLevelFunction() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              @Throws(IOException::class)
              fun foo() {
              }
              """,
            """
              fun foo() {
              }
              """
          )
        );
    }

    @Test
    void leaveUnrelatedThrowsAnnotationUntouched() {
        rewriteRun(
          kotlin(
            """
              class Foo {
                  @Throws(RuntimeException::class)
                  fun foo() {
                  }
              }
              """
          )
        );
    }
}
