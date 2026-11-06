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
import org.openrewrite.coldfusion.style.CFFormat;
import org.openrewrite.coldfusion.style.WrappingAndBracesStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.UnaryOperator;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Only line breaks change here. Indentation is the {@link TabsAndIndentsVisitor}'s.
 */
class WrappingAndBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(CFFormat.wrappingAndBraces())));
    }

    private static RecipeSpec style(RecipeSpec spec, UnaryOperator<WrappingAndBracesStyle> style) {
        return spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(style.apply(CFFormat.wrappingAndBraces()))));
    }

    @Test
    void bracesOnTheLineBefore() {
        rewriteRun(
          cfc(
            """
              component
              {
                  function f()
                  {
                      if (x)
                      {
                          y = 1;
                      }
                      else
                      {
                          y = 2;
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      if (x) {
                          y = 1;
                      } else {
                          y = 2;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void bracesAndKeywordsOnLinesOfTheirOwn() {
        rewriteRun(
          spec -> style(spec, s -> s
            .withBraces(s.getBraces().withFunctionOnNewLine(true).withStatementOnNewLine(true))
            .withKeywordsAfterBlock(s.getKeywordsAfterBlock().withElseOnNewLine(true).withCatchOnNewLine(true))),
          cfc(
            """
              component {
                  function f() {
                      try {
                          y = 1;
                      } catch (any e) {
                          y = 2;
                      }
                  }
              }
              """,
            """
              component {
                  function f()
              {
                      try
              {
                          y = 1;
                      }
              catch (any e)
              {
                          y = 2;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void statementsARecipeBuiltOnOneLineGetLinesOfTheirOwn() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {return 1;}
              }
              """,
            """
              component {
                  function f() {
              return 1;
              }
              }
              """
          )
        );
    }

    @Test
    void statementsAnAuthorPutOnOneLineStay() {
        rewriteRun(
          spec -> style(spec, s -> s.withKeepSimpleBlocksInOneLine(true)),
          cfc(
            """
              component {
                  function f() {
                      var a = 1; var b = 2;
                      if (a) { return b; }
                  }
              }
              """
          )
        );
    }

    @Test
    void simpleBlockLaidOutWhenTheStyleDoesNotKeepIt() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      if (a) { return b; }
                  }
              }
              """,
            """
              component {
                  function f() {
                      if (a) {
              return b;
              }
                  }
              }
              """
          )
        );
    }

    @Test
    void tagsKeepTheirLines() {
        rewriteRun(
          cfm(
            """
              <cfif x><cfset y = 1></cfif>
              <cfsilent><cfset z = 2><cfset w = 3></cfsilent>
              """
          )
        );
    }
}
