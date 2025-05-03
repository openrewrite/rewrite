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

class EqualsMethodUsageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EqualsMethodUsage());
    }

    @DocumentExample
    @Test
    void replace() {
        rewriteRun(
          kotlin(
            """
              fun isSame(obj1 : String, obj2: String) : Boolean {
                  val isSame = obj1.equals(obj2)
              }
              """,
            """
              fun isSame(obj1 : String, obj2: String) : Boolean {
                  val isSame = obj1 == obj2
              }
              """
          )
        );
    }

    @Test
    void replaceWithComment() {
        rewriteRun(
          kotlin(
            """
              fun isSame(obj1 : String, obj2: String) : Boolean {
                  val isSame = obj1.equals( /*comment*/ obj2)
              }
              """,
            """
              fun isSame(obj1 : String, obj2: String) : Boolean {
                  val isSame = obj1 == /*comment*/ obj2
              }
              """
          )
        );
    }

    @Test
    void replaceWithNotEqual() {
        rewriteRun(
          kotlin(
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = !obj1.equals(obj2)
              }
              """,
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = obj1 != obj2
              }
              """
          )
        );
    }

    @Test
    void replaceWithNotEqualWithComments() {
        rewriteRun(
          kotlin(
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = !obj1.equals( /*comment*/ obj2)
              }
              """,
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = obj1 != /*comment*/ obj2
              }
              """
          )
        );
    }

    @Test
    void replaceWithNotEqualInParentheses() {
        rewriteRun(
          kotlin(
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = !(obj1.equals(obj2))
              }
              """,
            """
              fun method(obj1 : String, obj2: String) {
                  val isNotSame = obj1 != obj2
              }
              """
          )
        );
    }

    @Test
    void equalsInBlock() {
        rewriteRun(
          kotlin(
            """
              val v = print(listOf("1").filter { e -> e.equals("1") })
              """,
            """
              val v = print(listOf("1").filter { e -> e == "1" })
              """
          )
        );
    }
}
