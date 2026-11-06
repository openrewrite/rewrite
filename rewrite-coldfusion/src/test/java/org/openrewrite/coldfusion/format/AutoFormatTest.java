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
import org.openrewrite.ExecutionContext;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AutoFormatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @Test
    void scriptComponent() {
        rewriteRun(
          cfc(
            """
              component extends = "Base"   accessors=true{
              property name="dao"  inject="DAO";
                    public any function init(required string name,numeric count=0){
              var x=[1,2,3];
                        var s={a:1,"b"=2};
                local.y=x[1]+s.a*2;
                      if(x.len()>2){
              writeOutput("big");
                      }
                      else if (y EQ 1)
                      {
                          y++;
                      }
                      var f = function(a,b){return a&b;};
                      return this;
                  }
              }
              """,
            """
              component extends="Base" accessors=true {
                  property name="dao" inject="DAO";

                  public any function init(required string name, numeric count = 0) {
                      var x = [1, 2, 3];
                      var s = {a: 1, "b" = 2};
                      local.y = x[1] + s.a * 2;
                      if (x.len() > 2) {
                          writeOutput("big");
                      } else if (y EQ 1) {
                          y++;
                      }
                      var f = function(a, b) {
                          return a & b;
                      };
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void tagComponentWhoseFunctionsAreSilent() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cfproperty name = "greeting"   type="string">

                      <cffunction name="greet"   access="public" output="false" >
              <cfargument name="who" type="string" required="true" >
                          <cfset var x=1>
                  <cfif x GT 1>
              <cfset x = 2>
                    <cfelse>
                          <cfset x = 3>
                          </cfif>
              <cfreturn variables.greeting & ", " & arguments.who >
              </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cfproperty name="greeting" type="string">

                  <cffunction name="greet" access="public" output="false">
                      <cfargument name="who" type="string" required="true">
                      <cfset var x = 1>
                      <cfif x GT 1>
                          <cfset x = 2>
                      <cfelse>
                          <cfset x = 3>
                      </cfif>
                      <cfreturn variables.greeting & ", " & arguments.who>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void keepsWhatATemplateWrites() {
        rewriteRun(
          cfm(
            """
              <html>
                    <cfif x>
              <pre>
                keep    this
              </pre>
                  </cfif>
              <cfset y=1>   \s
              <cfoutput>
                      #y#
              </cfoutput>
              </html>
              """,
            """
              <html>
                    <cfif x>
              <pre>
                keep    this
              </pre>
                  </cfif>
              <cfset y = 1>   \s
              <cfoutput>
                      #y#
              </cfoutput>
              </html>
              """
          )
        );
    }

    @Test
    void keepsWhatAFunctionThatOutputsWrites() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cffunction name="render">
                    <cfif true>
                  text
                    </cfif>
              </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void indentsWhereTemplateIsSilent() {
        rewriteRun(
          cfm(
            """
              <cfsilent>
              <cfif x>
              <cfset y=1>
              </cfif>
              </cfsilent>
              <p>#y#</p>
              """,
            """
              <cfsilent>
                  <cfif x>
                      <cfset y = 1>
                  </cfif>
              </cfsilent>
              <p>#y#</p>
              """
          )
        );
    }

    @Test
    void indentsAfterOutputOnlySettingAndKeepsOutputBodies() {
        rewriteRun(
          cfm(
            """
              <cfsetting enablecfoutputonly="true">
              <cfif x>
              <cfset y = 1>
              </cfif>
              <cfoutput>
                <b>#y#</b>
              </cfoutput>
              <cfsetting enablecfoutputonly="false">
              """,
            """
              <cfsetting enablecfoutputonly="true">
              <cfif x>
                  <cfset y = 1>
              </cfif>
              <cfoutput>
                <b>#y#</b>
              </cfoutput>
              <cfsetting enablecfoutputonly="false">
              """
          )
        );
    }

    @Test
    void cfscriptInATemplate() {
        rewriteRun(
          cfm(
            """
              <p>Hi</p>
              <cfscript>
                x=1;
                    if(x){y=2;}
              </cfscript>
              """,
            """
              <p>Hi</p>
              <cfscript>
              x = 1;
              if (x) {
                  y = 2;
              }
              </cfscript>
              """
          )
        );
    }

    @Test
    void neverChangesStringsOrComments() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      var s = "a  =  b   #x  +  y#";
                      /*  keep   this  */
                      return s;
                  }
              }
              """
          )
        );
    }

    @Test
    void formatsWhatARecipeAdds() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration &&
                      b.getStatements().size() == 1) {
                      // a copy of the statement with no whitespace at all, as a recipe builds one
                      Statement copy = (Statement) new StripWhitespace().visitNonNull(b.getStatements().get(0), ctx);
                      List<Statement> statements = new ArrayList<>(b.getStatements());
                      statements.add(copy.withId(randomId()));
                      J.Block added = b.withStatements(statements);
                      return maybeAutoFormat(b, added, ctx);
                  }
                  return b;
              }
          })),
          cfc(
            """
              component {
                  function f() {
                      if (a) {
                          return x + 1;
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      if (a) {
                          return x + 1;
                      }
                      if (a) {
                          return x + 1;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void autoIndentMovesWhatARecipeMoves() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  J.If i = super.visitIf(iff, ctx);
                  J.Block then = (J.Block) i.getThenPart();
                  List<Statement> statements = then.getStatements();
                  if (statements.size() == 1 && statements.get(0) instanceof J.Block) {
                      // lift the statements of a nested block into the if's own block
                      J.Block lifted = then.getPadding().withStatements(((J.Block) statements.get(0)).getPadding().getStatements());
                      return i.withThenPart(autoIndent(lifted, ctx, getCursor()));
                  }
                  return i;
              }
          })),
          cfc(
            """
              component {
                  function f() {
                      if (a) {
                          {
                              x = 1;
                              y = foo(
                                  x
                              );
                          }
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      if (a) {
                          x = 1;
                          y = foo(
                              x
                          );
                      }
                  }
              }
              """
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
