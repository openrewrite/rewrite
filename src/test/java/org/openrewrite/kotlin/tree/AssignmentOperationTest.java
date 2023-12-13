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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"KotlinConstantConditions", "ConstantConditionIf"})
class AssignmentOperationTest implements RewriteTest {

    @Test
    void minusEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 0
                n -= 5
              }
              """
          )
        );
    }

    @Test
    void plusEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 0
                n += 5
              }
              """
          )
        );
    }

    @Test
    void timesEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 0
                n *= 5
              }
              """
          )
        );
    }

    @Test
    void divideEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 0
                n /= 5
              }
              """
          )
        );
    }

    @Test
    void conditionalAssignment() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 84
                n /= if (true) 42 else 21
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/268")
    void augmentedAssign() {
        rewriteRun(
          kotlin(
            """
              operator fun <T> MutableCollection<T>.divAssign(elements: Collection<T>) {
                  this.retainAll(elements)
              }

              fun names(): Set<String> {
                  val result = HashSet<String>()
                  result += "x"
                  result -= "x"
                  result /= setOf("x")
                  return result
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/305")
    void augmentedAssignmentAnnotation() {
        rewriteRun(
          // Type validation is disabled due to https://github.com/openrewrite/rewrite-kotlin/issues/511
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlin(
            """
             fun foo(l: MutableList<String>) {
                 @Suppress
                 l += "x"
             }
             """
          )
        );
    }
}
