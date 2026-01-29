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
package org.openrewrite.kotlin.replace;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ReplaceKotlinMethodTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceCharToIntWithCode() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "code",
            null,
            null
          )),
          kotlin(
            """
              fun test(c: Char): Int {
                  return c.toInt()
              }
              """,
            """
              fun test(c: Char): Int {
                  return c.code
              }
              """
          )
        );
    }

    @Test
    void replaceWithExplicitThisReference() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "this.code",
            null,
            null
          )),
          kotlin(
            """
              fun test(c: Char): Int {
                  return c.toInt()
              }
              """,
            """
              fun test(c: Char): Int {
                  return c.code
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenMethodDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "code",
            null,
            null
          )),
          kotlin(
            """
              fun test(i: Int): String {
                  return i.toString()
              }
              """
          )
        );
    }

    @Test
    void replacePropertyStyleReplacement() {
        // Tests replacing a method with a property-style access
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "this.code",
            null,
            null
          )),
          kotlin(
            """
              fun digitValue(c: Char): Int {
                  return c.toInt() - '0'.toInt()
              }
              """,
            """
              fun digitValue(c: Char): Int {
                  return c.code - '0'.code
              }
              """
          )
        );
    }

    @Test
    void replaceMultipleOccurrences() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "code",
            null,
            null
          )),
          kotlin(
            """
              fun test(a: Char, b: Char): Int {
                  return a.toInt() + b.toInt()
              }
              """,
            """
              fun test(a: Char, b: Char): Int {
                  return a.code + b.code
              }
              """
          )
        );
    }

    @Test
    void replaceConstructorCall() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "Holder <init>(kotlin.Int)",
            "listOf(value)",
            null,
            null
          )).typeValidationOptions(TypeValidation.all().methodInvocations(false)),
          kotlin(
            """
              class Holder(val value: Int)
              """),
          kotlin(
            """
              val h = Holder(42)
              """,
            """
              val h = listOf(42)
              """
          )
        );
    }

    @Test
    void preservesWhitespaceAndFormatting() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceKotlinMethod(
            "kotlin.Char toInt()",
            "code",
            null,
            null
          )),
          kotlin(
            """
              fun test(c: Char): Int {
                  val result =
                      c.toInt()
                  return result
              }
              """,
            """
              fun test(c: Char): Int {
                  val result =
                      c.code
                  return result
              }
              """
          )
        );
    }
}
