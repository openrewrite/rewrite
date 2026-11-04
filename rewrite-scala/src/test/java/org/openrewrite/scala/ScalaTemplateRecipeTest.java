/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ScalaTemplateRecipeTest implements RewriteTest {

    static class ReplaceEqualsOne extends ScalaTemplateRecipe {
        @Override public String getDisplayName() { return "Replace == 1 with constant"; }
        @Override public String getDescription() { return "Replace x == 1 with true."; }
        @Override protected String[] getBeforeTemplates() {
            return new String[]{ "#{a:any(int)} == 1" };
        }
        @Override protected String getAfterTemplate() {
            return "true";
        }
    }

    @DocumentExample
    @Test
    void replaceMatchedExpression() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceEqualsOne()),
          scala(
            """
              class Test {
                  val b1 = 5 == 1
                  val b2 = 5 == 2
              }
              """,
            """
              class Test {
                  val b1 = true
                  val b2 = 5 == 2
              }
              """
          ));
    }

    static class NormalizeComparison extends ScalaTemplateRecipe {
        @Override public String getDisplayName() { return "Normalize comparison"; }
        @Override public String getDescription() { return "Replace < 1 or == 0 with false."; }
        @Override protected String[] getBeforeTemplates() {
            return new String[]{
              "#{a:any(int)} < 1",
              "#{a:any(int)} == 0"
            };
        }
        @Override protected String getAfterTemplate() {
            return "false";
        }
    }

    @Test
    void multipleBeforeTemplates() {
        rewriteRun(
          spec -> spec.recipe(new NormalizeComparison()),
          scala(
            """
              class Test {
                  val a = 5 < 1
                  val b = 5 == 0
                  val c = 5 > 1
              }
              """,
            """
              class Test {
                  val a = false
                  val b = false
                  val c = 5 > 1
              }
              """
          ));
    }

    @Test
    void noMatchNoChange() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceEqualsOne()),
          scala(
            """
              class Test {
                  val x = 42
              }
              """
          ));
    }
}
