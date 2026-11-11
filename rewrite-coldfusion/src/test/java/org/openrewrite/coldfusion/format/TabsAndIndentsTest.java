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
import org.openrewrite.coldfusion.style.TabsAndIndentsStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.UnaryOperator;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

class TabsAndIndentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(CFFormat.tabsAndIndents())));
    }

    private static RecipeSpec style(RecipeSpec spec, UnaryOperator<TabsAndIndentsStyle> style) {
        return spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(style.apply(CFFormat.tabsAndIndents()))));
    }

    @Test
    void scriptStatements() {
        rewriteRun(
          cfc(
            """
              component {
                function f() {
                        var x = 1;
                  if (x) {
                  return x;
                  } else {
                        return 0;
                  }
                }
              }
              """,
            """
              component {
                  function f() {
                      var x = 1;
                      if (x) {
                          return x;
                      } else {
                          return 0;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tabs() {
        rewriteRun(
          spec -> style(spec, s -> s.withUseTabCharacter(true)),
          cfc(
            """
              component {
                function f() {
                  return 1;
                }
              }
              """,
            """
              component {
              \tfunction f() {
              \t\treturn 1;
              \t}
              }
              """
          )
        );
    }

    @Test
    void continuationLinesMoveWithTheirStatement() {
        rewriteRun(
          cfc(
            """
              component {
                function f() {
                  var result = queryExecute(
                      "SELECT 1",
                      {},
                      { datasource: "app" }
                  );
                  var total = a +
                         b;
                }
              }
              """,
            """
              component {
                  function f() {
                      var result = queryExecute(
                          "SELECT 1",
                          {},
                          { datasource: "app" }
                      );
                      var total = a +
                             b;
                  }
              }
              """
          )
        );
    }

    @Test
    void operandsLineUpWithTheirExpression() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      if (
                          a &&
                          b
                      ) {
                          return x
                              ? 1
                              : 2;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void lineNoFurtherInThanItsStatementIsIndented() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      return foo(a,
                      b);
                  }
              }
              """,
            """
              component {
                  function f() {
                      return foo(a,
                          b);
                  }
              }
              """
          )
        );
    }

    @Test
    void closingBracketAlignsWithTheLineThatOpensIt() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      for (
                          var key in [
                              'a',
                              'b'
                                ]
                      ) {
                          x = key;
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      for (
                          var key in [
                              'a',
                              'b'
                          ]
                      ) {
                          x = key;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void closingBracketIndentedLikeTheList() {
        rewriteRun(
          spec -> style(spec, s -> s.withIndentClosingBrackets(true)),
          cfc(
            """
              component {
                  function f(
                      required string a,
                      required string b
                  ) {
                      return a;
                  }
              }
              """,
            """
              component {
                  function f(
                      required string a,
                      required string b
                      ) {
                      return a;
                  }
              }
              """
          )
        );
    }

    @Test
    void switchCases() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      switch (x) {
                      case 1:
                      y = 2;
                      break;
                      default:
                      y = 3;
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      switch (x) {
                          case 1:
                              y = 2;
                              break;
                          default:
                              y = 3;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void docCommentMovesWithItsFunction() {
        rewriteRun(
          cfc(
            """
              component {
                      /**
                       * Says hi.
                       */
                      function f() {
                      }
              }
              """,
            """
              component {
                  /**
                   * Says hi.
                   */
                  function f() {
                  }
              }
              """
          )
        );
    }

    @Test
    void commentInTheFirstColumnStays() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
              //        x = 1;
                        y = 2;
                  }
              }
              """,
            """
              component {
                  function f() {
              //        x = 1;
                      y = 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void tagComponentWithSilentFunctions() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cffunction name="f" output="false">
              <cfargument name="a">
              <cfif a>
              <cfset b = 1>
              <cfelseif a GT 1>
              <cfset b = 2>
              <cfelse>
              <cfset b = 3>
              </cfif>
              <cftry>
              <cfset c = 1>
              <cfcatch type="any">
              <cfset c = 2>
              </cfcatch>
              </cftry>
              </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cfargument name="a">
                      <cfif a>
                          <cfset b = 1>
                      <cfelseif a GT 1>
                          <cfset b = 2>
                      <cfelse>
                          <cfset b = 3>
                      </cfif>
                      <cftry>
                          <cfset c = 1>
                          <cfcatch type="any">
                              <cfset c = 2>
                          </cfcatch>
                      </cftry>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void tagBodiesAsTheStyleSays() {
        rewriteRun(
          spec -> style(spec, s -> s.withTags(s.getTags().withIndentComponentBody(false).withIndentCatchFromTry(false)
            .withIndentCaseFromSwitch(false))),
          cfc(
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cftry>
                          <cfswitch expression="#x#">
                              <cfcase value="1">
                                  <cfset y = 1>
                              </cfcase>
                          </cfswitch>
                          <cfcatch>
                              <cfset y = 2>
                          </cfcatch>
                      </cftry>
                  </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
              <cffunction name="f" output="false">
                  <cftry>
                      <cfswitch expression="#x#">
                      <cfcase value="1">
                          <cfset y = 1>
                      </cfcase>
                      </cfswitch>
                  <cfcatch>
                      <cfset y = 2>
                  </cfcatch>
                  </cftry>
              </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void scriptBodyIndentedWhenTheStyleSays() {
        rewriteRun(
          spec -> style(spec, s -> s.withTags(s.getTags().withIndentScriptBody(true))),
          cfm(
            """
              <cfscript>
              x = 1;
              if (x) {
              y = 2;
              }
              </cfscript>
              """,
            """
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
    void silentBody() {
        rewriteRun(
          cfm(
            """
              <cfsilent>
              <cfset x = 1>
              <cfif x>
              <cfset y = 2>
              </cfif>
              </cfsilent>
              """,
            """
              <cfsilent>
                  <cfset x = 1>
                  <cfif x>
                      <cfset y = 2>
                  </cfif>
              </cfsilent>
              """
          )
        );
    }

    @Test
    void silentBodyFlatWhenTheStyleSays() {
        rewriteRun(
          spec -> style(spec, s -> s.withTags(s.getTags().withIndentSilentBody(false))),
          cfm(
            """
              <cfsilent>
                  <cfset x = 1>
              </cfsilent>
              """,
            """
              <cfsilent>
              <cfset x = 1>
              </cfsilent>
              """
          )
        );
    }

    @Test
    void keepsTheIndentationATemplateWrites() {
        rewriteRun(
          cfm(
            """
              <cfif x>
              <cfset y = 1>
                    <cfloop from="1" to="3" index="i">
              #i#
                    </cfloop>
              </cfif>
              """
          ),
          cfc(
            """
              <cfcomponent>
              <cffunction name="f">
                    <cfset x = 1>
              </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void keepsTheIndentationOfABodyWithText() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cffunction name="f" output="false">
                  <cfif x>
                          <p>never written</p>
                      <cfset y = 1>
                  </cfif>
              </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                  <cfif x>
                          <p>never written</p>
                      <cfset y = 1>
                  </cfif>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void tagWhoseEndTagStaysKeepsItsStartTagAlignedWithIt() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cffunction name="f" output="false">
                          <cfsavecontent variable="html">
                              <b>kept</b>
                          </cfsavecontent>
                          <cfreturn html>
              </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                          <cfsavecontent variable="html">
                              <b>kept</b>
                          </cfsavecontent>
                      <cfreturn html>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void attributesOnLinesOfTheirOwn() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
              <cffunction name="f"
              output="false"
                          access="public"
              >
              <cfreturn 1>
              </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f"
                      output="false"
                              access="public"
                  >
                      <cfreturn 1>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }
}
