/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.coldfusion.style.BlankLinesStyle;
import org.openrewrite.coldfusion.style.CFFormat;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.UnaryOperator;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

class BlankLinesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new BlankLinesVisitor<>(CFFormat.blankLines())));
    }

    private static RecipeSpec style(RecipeSpec spec, UnaryOperator<BlankLinesStyle> style) {
        return spec.recipe(toRecipe(() -> new BlankLinesVisitor<>(style.apply(CFFormat.blankLines()))));
    }

    @Test
    void keepsAtMostTheMaximum() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      x = 1;



                      y = 2;


                  }
              }
              """,
            """
              component {
                  function f() {
                      x = 1;


                      y = 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void blankLineAroundFunctions() {
        rewriteRun(
          cfc(
            """
              component {
                  property name="a";
                  function f() {
                  }
                  // g
                  function g() {
                  }
              }
              """,
            """
              component {
                  property name="a";

                  function f() {
                  }

                  // g
                  function g() {
                  }
              }
              """
          )
        );
    }

    @Test
    void silentTagBodies() {
        rewriteRun(
          spec -> style(spec, s -> s.withKeepMaximum(s.getKeepMaximum().withInCode(1))),
          cfc(
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cfset x = 1>



                      <cfset y = 2>
                  </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cfset x = 1>

                      <cfset y = 2>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void keepsTheBlankLinesATemplateWrites() {
        rewriteRun(
          cfm(
            """
              <cfset x = 1>




              <cfset y = 2>
              """
          )
        );
    }
}
