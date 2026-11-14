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
import org.openrewrite.coldfusion.style.SpacesStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.UnaryOperator;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SpacesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new SpacesVisitor<>(CFFormat.spaces())));
    }

    private static RecipeSpec style(RecipeSpec spec, UnaryOperator<SpacesStyle> style) {
        return spec.recipe(toRecipe(() -> new SpacesVisitor<>(style.apply(CFFormat.spaces()))));
    }

    @Test
    void operators() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      x=a+b*c&d;
                      y   =   x?1:2;
                      z = !x && y  EQ  z;
                      w+=1;
                      s &= "x";
                      i++;
                      g = (a)=>a;
                  }
              }
              """,
            """
              component {
                  function f() {
                      x = a + b * c & d;
                      y = x ? 1 : 2;
                      z = !x && y EQ z;
                      w += 1;
                      s &= "x";
                      i++;
                      g = (a) => a;
                  }
              }
              """
          )
        );
    }

    @Test
    void tightOperatorsNeverRunTogether() {
        rewriteRun(
          spec -> style(spec, s -> s.withAroundOperators(s.getAroundOperators().withBinary(false).withAssignment(false))),
          cfc(
            """
              component {
                  function f() {
                      x = a - -1;
                      y = a + ++b;
                      z = a EQ b;
                      w = !!a;
                  }
              }
              """,
            """
              component {
                  function f() {
                      x=a- -1;
                      y=a+ ++b;
                      z=a EQ b;
                      w=!!a;
                  }
              }
              """
          )
        );
    }

    @Test
    void parenthesesAndCalls() {
        rewriteRun(
          cfc(
            """
              component {
                  function f ( a , b ) {
                      foo ( a,b );
                      bar( );
                      if(x) {}
                      while ( x ) {}
                      return ( a + b );
                  }
              }
              """,
            """
              component {
                  function f(a, b) {
                      foo(a, b);
                      bar();
                      if (x) {}
                      while (x) {}
                      return (a + b);
                  }
              }
              """
          )
        );
    }

    @Test
    void paddedParenthesesAsOrtusWritesThem() {
        rewriteRun(
          spec -> style(spec, s -> s
            .withWithin(s.getWithin().withFunctionCallParentheses(true).withFunctionDeclarationParentheses(true)
              .withKeywordParentheses(true).withBrackets(true).withArrayLiteral(true).withStructLiteral(true))
            .withOther(s.getOther().withBeforeFunctionBrace(false).withBeforeStatementBrace(false))),
          cfc(
            """
              component {
                  function f(a, b) {
                      if (x[1]) {
                          return foo([1, 2], {a: 1});
                      }
                  }
              }
              """,
            """
              component {
                  function f( a, b ){
                      if ( x[ 1 ] ){
                          return foo( [ 1, 2 ], { a: 1 } );
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void structsAndArrays() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      var s = { a :1, "b"=2 };
                      var t = [ 1,2 ];
                      var e = { };
                      foo(a=1, b = 2);
                  }
              }
              """,
            """
              component {
                  function f() {
                      var s = {a: 1, "b" = 2};
                      var t = [1, 2];
                      var e = {};
                      foo(a = 1, b = 2);
                  }
              }
              """
          )
        );
    }

    @Test
    void forLoops() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      for(var i=1;i<=10;i++) {}
                      for( var item in items ) {}
                  }
              }
              """,
            """
              component {
                  function f() {
                      for (var i = 1; i <= 10; i++) {}
                      for (var item in items) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void bracesAndKeywordsAfterBlocks() {
        rewriteRun(
          cfc(
            """
              component{
                  function f(){
                      try{
                          x = 1;
                      }catch(any e){
                          x = 2;
                      }finally{
                          x = 3;
                      }
                      if (x){
                      }else{
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      try {
                          x = 1;
                      } catch (any e) {
                          x = 2;
                      } finally {
                          x = 3;
                      }
                      if (x) {
                      } else {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void elseIfKeepsOneSpace() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      if (a) {
                      } elseif(b) {
                      }
                  }
              }
              """,
            """
              component {
                  function f() {
                      if (a) {
                      } elseif (b) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void declarationAttributes() {
        rewriteRun(
          cfc(
            """
              component   extends = "Base"  accessors = true {
                  property  name = "dao"   inject = "DAO";
                  public   string   function   f()   output = false {
                      return "";
                  }
              }
              """,
            """
              component extends="Base" accessors=true {
                  property name="dao" inject="DAO";
                  public string function f() output=false {
                      return "";
                  }
              }
              """
          )
        );
    }

    @Test
    void alignedAttributesAndAssignmentsKeptWhenTheStyleSays() {
        rewriteRun(
          spec -> style(spec, s -> s.withOther(s.getOther().withKeepAlignedAssignments(true))
            .withTags(s.getTags().withKeepAlignedAttributes(true))),
          cfc(
            """
              component {
                  property name="userService"   inject="UserService";
                  property name="log"           inject="logbox:logger:{this}";

                  function f() {
                      this.title       = "Module";
                      this.author      = "Someone";
                  }
              }
              """
          )
        );
    }

    @Test
    void tagAttributesAndExpressions() {
        rewriteRun(
          cfm(
            """
              <cfset   x=1 >
              <cfif x GT 1 >
              <cfparam  name = "url.id"   default="0" />
              </cfif >
              """,
            """
              <cfset x = 1>
              <cfif x GT 1>
              <cfparam name="url.id" default="0"/>
              </cfif>
              """
          )
        );
    }

    @Test
    void spaceBeforeSelfClosingEndWhenTheStyleSays() {
        rewriteRun(
          spec -> style(spec, s -> s.withTags(s.getTags().withBeforeSelfClosingEnd(true))),
          cfc(
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cfset x = 1/>
                      <cfreturn x>
                  </cffunction>
              </cfcomponent>
              """,
            """
              <cfcomponent output="false">
                  <cffunction name="f" output="false">
                      <cfset x = 1 />
                      <cfreturn x>
                  </cffunction>
              </cfcomponent>
              """
          )
        );
    }

    @Test
    void neverChangesTheWhitespaceBetweenTagsOrInsideStringsAndInterpolations() {
        rewriteRun(
          cfm(
            """
              <p>  <cfoutput>  #a+b#  </cfoutput>  </p>
              <cfset s = "a  =  #x  +  y#">
              """
          )
        );
    }

    @Test
    void leadingCommasKeepTheirSpacing() {
        rewriteRun(
          cfc(
            """
              component {
                  function f() {
                      var paths = [
                          "a"
                          ,"b"
                          , "c"
                      ];
                  }
              }
              """
          )
        );
    }
}
