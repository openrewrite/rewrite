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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * A tree whose whitespace is all removed, as a recipe builds one, still prints as CFML that parses to it.
 */
class MinimumViableSpacingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // stripping the whitespace again would make an equal copy of what the first cycle printed
        spec.cycles(1).expectedCyclesThatMakeChanges(1).recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Cf.CompilationUnit) {
                    J stripped = new StripWhitespace().visit(tree, ctx);
                    return new MinimumViableSpacingVisitor<ExecutionContext>().visit(stripped, ctx);
                }
                return super.visit(tree, ctx);
            }
        }));
    }

    @Test
    void keywordsThatDoubleAsNames() {
        rewriteRun(
          cfc(
            """
              component {
                  public string function f(required string name) output=false {
                      var x = new models.User();
                      var local.y = 1;
                      if (x) {
                          return x;
                      } else if (y) {
                          throw "z";
                      }
                      for (var item in items) {}
                      switch (x) {
                          case 1:
                              break;
                          default:
                      }
                      return;
                  }
              }
              """,
            """
              component{public string function f(required string name) output=false{var x=new models.User();var local.y=1;if(x){return x;}else if(y){throw "z";}for(var item in items){}switch(x){case 1:break;default:}return;}}"""
          )
        );
    }

    @Test
    void spelledOperators() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      return a EQ b AND NOT c MOD 2 CONTAINS "x";
                  }
              }
              """,
            """
              component{function f(){return a EQ b AND NOT c MOD 2 CONTAINS "x";}}"""
          )
        );
    }

    @Test
    void operatorsThatWouldRunTogether() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      return a - -1 + ++b;
                  }
              }
              """,
            """
              component{function f(){return a- -1+ ++b;}}"""
          )
        );
    }

    @Test
    void tags() {
        rewriteRun(
          cfm(
            """
              <cfset var x = 1>
              <cfif x GT 1><cfreturn x></cfif>
              <cfparam name="url.id" default="0">
              """,
            """
              <cfset var x=1><cfif x GT 1><cfreturn x></cfif><cfparam name="url.id" default="0">"""
          ),
          cfc(
            """
              <cfcomponent output="false">
                  <cffunction name="f" access="public" output="false">
                      <cfargument name="a" type="string" required="true">
                  </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false"><cffunction name="f" access="public" output="false"><cfargument name="a" type="string" required="true"></cffunction></cfcomponent>""",
            spec -> spec.path("Tags.cfc")
          )
        );
    }

    /**
     * Removes every space, as a tree built by a recipe has none.
     */
    private static class StripWhitespace extends ColdFusionIsoVisitor<ExecutionContext> {
        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
            return Space.EMPTY;
        }

        @Override
        public Space visitSpace(Space space, CfSpace.Location loc, ExecutionContext ctx) {
            return Space.EMPTY;
        }
    }
}
