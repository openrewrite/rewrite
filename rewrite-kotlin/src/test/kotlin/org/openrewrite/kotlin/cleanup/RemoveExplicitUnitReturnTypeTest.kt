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
package org.openrewrite.kotlin.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.DocumentExample
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class RemoveExplicitUnitReturnTypeTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RemoveExplicitUnitReturnType).validateRecipeSerialization(false)
    }

    @DocumentExample
    @Test
    fun `block body`() = rewriteRun(
        kotlin(
            "fun foo(): Unit {}",
            "fun foo() {}"
        )
    )

    @Test
    fun `expression body`() = rewriteRun(
        kotlin(
            "fun foo(): Unit = println(\"hi\")",
            "fun foo() = println(\"hi\")"
        )
    )

    @Test
    fun `with parameters`() = rewriteRun(
        kotlin(
            "fun foo(x: Int): Unit {}",
            "fun foo(x: Int) {}"
        )
    )

    @Test
    fun `extension function`() = rewriteRun(
        kotlin(
            "fun String.foo(): Unit {}",
            "fun String.foo() {}"
        )
    )

    @Test
    fun `override modifier preserved`() = rewriteRun(
        kotlin(
            """
              open class Base {
                  open fun foo(): Unit {
                  }
              }
              class Derived : Base() {
                  override fun foo(): Unit {
                  }
              }
              """,
            """
              open class Base {
                  open fun foo() {
                  }
              }
              class Derived : Base() {
                  override fun foo() {
                  }
              }
              """
        )
    )

    @Test
    fun `does not change other return types`() = rewriteRun(
        kotlin(
            "fun foo(): Int { return 1 }"
        )
    )

    @Test
    fun `does not change already implicit return type`() = rewriteRun(
        kotlin(
            "fun foo() {}"
        )
    )

    @Test
    fun `does not change nullable unit`() = rewriteRun(
        kotlin(
            "fun foo(): Unit? { return null }"
        )
    )
}
