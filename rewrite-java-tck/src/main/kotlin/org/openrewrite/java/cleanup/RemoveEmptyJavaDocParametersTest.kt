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

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("JavadocDeclaration", "UnnecessarySemicolon")
interface RemoveEmptyJavaDocParametersTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RemoveEmptyJavaDocParameters())
    }

    @Test
    fun singleLineParam() =
      rewriteRun(
        java(
          """
            class Test {
                /**@param arg0*/
                void method(int arg0) {
                }
            }
          """,
          """
            class Test {
                /***/
                void method(int arg0) {
                }
            }
          """
        )
      )

    @Test
    fun removeParamWithNoPrefix() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 *@param arg0
                 */
                void method(int arg0) {
                }
            }
          """,
          """
            class Test {
                /**
                 */
                void method(int arg0) {
                }
            }
          """
        )
      )

    @Test
    fun removeEmptyParams() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 * @param arg0 description1
                 * @param arg1
                 * @param arg2 description3
                 */
                void method(int arg0, int arg1, int arg2) {
                }
            }
          """,
            """
            class Test {
                /**
                 * @param arg0 description1
                 * @param arg2 description3
                 */
                void method(int arg0, int arg1, int arg2) {
                }
            }
            """
        )
      )

    @Test
    fun multipleEmptyLines() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 * @param arg0
                 * 
                 * 
                 * 
                 * @param arg1 description
                 */
                void method(int arg0, int arg1) {
                }
            }
          """,
          """
            class Test {
                /**
                 * 
                 * 
                 * 
                 * @param arg1 description
                 */
                void method(int arg0, int arg1) {
                }
            }
          """
        )
      )

    @Test
    fun emptyReturn() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 * @return
                 */
                int method() {
                }
            }
          """,
          """
            class Test {
                /**
                 */
                int method() {
                }
            }
          """
        )
      )

    @Test
    fun emptyThrows() =
      rewriteRun(
        java(
          """
          class Test {
              /**
               * @throws
               */
              void method() throws IllegalStateException {
              }
          }
        """,
        """
          class Test {
              /**
               */
              void method() throws IllegalStateException {
              }
          }
        """
        )
      )

    @Test
    fun emptyThrowsOnFirstLine() =
      rewriteRun(
        java(
          """
            class Test {
                /** @throws*/
                void method() throws IllegalStateException {
                }
            }
          """,
          """
            class Test {
                /***/
                void method() throws IllegalStateException {
                }
            }
          """
        )
      )
}
