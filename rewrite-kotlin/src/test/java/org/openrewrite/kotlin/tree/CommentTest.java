/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class CommentTest implements RewriteTest {

    @Test
    void propertyTrailingComments() {
        rewriteRun(
          kotlin(
            """
              class T {
                  val x = 1 // comment 1
                  val y = 2 // comment 2
                  fun id() {} // comment 3
                  fun name() {} // comment 4
              }
              """
          )
        );
    }

    @Test
    void backToBackMultilineComments() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  /*
                   * C1
                   */
                  /*
                   * C2
                   */
              }
              """
          )
        );
    }

    @Test
    void multilineNestedInsideSingleLine() {
        rewriteRun(
          kotlin(
            """
              class Test { // /*
              }
              """
          )
        );
    }

    @Test
    void leadingComments() {
        rewriteRun(
          kotlin(
            """
              // C1

              // C2
              open class Test {
                  // C3

                  // C4
                  internal val n = 0

                  // C5

                  // C6
                  internal fun method() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/320")
    @Test
    void nestedComment() {
        rewriteRun(
          kotlin(
            """
              /* Outer C1
                  /**
                  * Inner C2
                  */
                 Outer C3
               */
              val a = 1
              """
          )
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "val foo = \"bar\"",
            "class Foo {}",
            "fun foo() {}",
            "@file:Suppress(\"PLATFORM_CLASS_MAPPED_TO_KOTLIN\", \"unused\")",
            "package foo /*c*/"
    })
    void multilineComments(String input) {
        rewriteRun(
          kotlin(
            """
              /*
               * Comment
               */
              %s
              """.formatted(input)
          )
        );
    }

    @Test
    void trailingComments() {
        rewriteRun(
          kotlin(
            """
              val n : Int = 10
              fun m ( ) : Int {
                return 1 + 1
              }
              class T ( val a : String ) {
              }
              """.replace("\n", "/**/\n").replace(" ", "/**/ ")
          )
        );
    }

    @Test
    void eof() {
        rewriteRun(
          kotlin(
            """
              class T () {
              }/**/
              """
          )
        );
    }

    @Test
    void commentAfterLambda() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val d = {it   : Int ->  /*c1*/   it + 42  /***/ } // comment
              }
              """
          )
        );
    }

    @Test
    void commentsBeforeCompanion() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun method(){} // END

                  /**
                   * Comment
                   */
                  companion  /*C2*/  object AssertValidQueryCompanion {
                  }
              }
              """
          )
        );
    }

    @Test
    void kdocBeforeValueParameter() {
        rewriteRun(
          kotlin(
            """
              class CodeGenConfig(
                  var kotlinAllFieldsOptional: Boolean = false,
                  /** If enabled, the names of the classes available via the DgsConstant class will be snake cased.*/
                  var snakeCaseConstantNames: Boolean = false
              )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/401")
    @ParameterizedTest
    @ValueSource(strings = {
      "var name: Boolean = true",
      "variable: Boolean = true"
    })
    void kdocBeforeValueParameter(String input) {
        rewriteRun(
          kotlin(
            """
              class Test(
                  /**
                   *
                   */
                  %s
              )
              """.formatted(input)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/415")
    @Test
    void kdocBeforeDestructuring() {
        rewriteRun(
          kotlin(
            """
              fun m() {
                  /** */
                  val (a, b) = Pair(1, 2)
               }
              """
          )
        );
    }
}
