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
package org.openrewrite.coldfusion.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;

class TemplateTest implements RewriteTest {

    @Test
    void textAndTags() {
        rewriteRun(
          cfm(
            """
              <html>
              <cfset x = 1>
              <p>Hello</p>
              </html>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = cu.getStatements();
                assertThat(statements).hasSize(3);
                assertThat(statements.get(0)).isInstanceOf(Cf.Text.class);
                Cf.Tag set = (Cf.Tag) statements.get(1);
                assertThat(set.getName()).isEqualTo("cfset");
                assertThat(set.getPrefix().getWhitespace()).isEqualTo("\n");
                assertThat(set.getExpression()).isInstanceOf(J.Assignment.class);
                assertThat(set.getBody()).isNull();
                Cf.Text text = (Cf.Text) statements.get(2);
                assertThat(text.getPrefix().getWhitespace()).isEqualTo("\n");
                assertThat(text.getText()).isEqualTo("<p>Hello</p>\n</html>");
            })
          )
        );
    }

    @Test
    void attributes() {
        rewriteRun(
          cfm(
            """
              <cfquery name="q" datasource = 'app' cachedwithin=#createTimeSpan(0, 1, 0, 0)# maxrows=10 debug>
                  SELECT * FROM users WHERE id = #url.id#
              </cfquery>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag query = (Cf.Tag) cu.getStatements().get(0);
                assertThat(query.getAttributes()).extracting(a -> a.getName().getSimpleName())
                  .containsExactly("name", "datasource", "cachedwithin", "maxrows", "debug");
                assertThat(query.getAttribute("NAME").getValue()).isInstanceOf(J.Literal.class);
                assertThat(query.getAttribute("cachedwithin").getValue()).isInstanceOf(Cf.StringTemplate.class);
                assertThat(query.getAttribute("debug").getValue()).isNull();
                assertThat(query.getBody()).isNotNull();
                assertThat(query.getBody().getStatements()).anyMatch(Cf.Interpolation.class::isInstance);
            })
          )
        );
    }

    @Test
    void ifElseIfElse() {
        rewriteRun(
          cfm(
            """
              <cfif x GT 1>
                  big
              <cfelseif x EQ 1>
                  one
              <cfelse>
                  small
              </cfif>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag cfif = (Cf.Tag) cu.getStatements().get(0);
                J.Binary condition = (J.Binary) cfif.getExpression();
                assertThat(condition.getOperator()).isEqualTo(J.Binary.Type.GreaterThan);
                assertThat(condition.getMarkers().findFirst(OperatorSpelling.class)).get()
                  .extracting(OperatorSpelling::getText).isEqualTo("GT");
                assertThat(cfif.getBody().getStatements())
                  .filteredOn(Cf.Tag.class::isInstance)
                  .extracting(t -> ((Cf.Tag) t).getName())
                  .containsExactly("cfelseif", "cfelse");
                assertThat(cfif.getClosing().getName()).isEqualTo("cfif");
            })
          )
        );
    }

    @Test
    void endTagInAnyCase() {
        rewriteRun(
          cfm(
            """
              <CFOUTPUT>#name#</cfOutput >
              """
          )
        );
    }

    @Test
    void selfClosingAndUnclosedTags() {
        rewriteRun(
          cfm(
            """
              <cfparam name="url.id" default="0" />
              <cfinclude template="header.cfm">
              <cfthrow message="nope"></cfthrow>
              <cf_layout title="Home">
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> tags = cu.getStatements().stream().filter(Cf.Tag.class::isInstance).toList();
                assertThat(((Cf.Tag) tags.get(0)).isSelfClosing()).isTrue();
                assertThat(((Cf.Tag) tags.get(1)).getBody()).isNull();
                assertThat(((Cf.Tag) tags.get(2)).getClosing()).isNotNull();
                assertThat(((Cf.Tag) tags.get(3)).getName()).isEqualTo("cf_layout");
            })
          )
        );
    }

    @Test
    void comments() {
        rewriteRun(
          cfm(
            """
              <!--- a CFML comment <!--- nested ---> --->
              <!-- an HTML comment, which is output -->
              <cfif true <!--- inside a tag --->>yes</cfif>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Text html = (Cf.Text) cu.getStatements().get(0);
                assertThat(html.getText()).isEqualTo("<!-- an HTML comment, which is output -->");
                assertThat(html.getPrefix().getComments()).singleElement()
                  .satisfies(c -> assertThat(((TagComment) c).getText()).isEqualTo(" a CFML comment <!--- nested ---> "));
            })
          )
        );
    }

    @Test
    void commentOpeningMoreThanItCloses() {
        rewriteRun(
          cfm(
            """
              <!--- an old block <!--- with a note --->
              <p>x</p>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Text text = (Cf.Text) cu.getStatements().get(0);
                assertThat(text.getText()).isEqualTo("<p>x</p>");
                assertThat(text.getPrefix().getComments()).singleElement()
                  .satisfies(c -> assertThat(((TagComment) c).getText()).isEqualTo(" an old block <!--- with a note "));
            })
          )
        );
    }

    @Test
    void spaceBetweenTagsIsPrefix() {
        rewriteRun(
          cfm(
            """
              <cfif x>
                  <cfset y = 1>
                  <!--- why --->
                  <cfoutput>Hi #name#!</cfoutput>
              </cfif>
              <!--- the end --->
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag cfif = (Cf.Tag) cu.getStatements().get(0);
                List<Statement> body = cfif.getBody().getStatements();
                assertThat(body).hasSize(2);
                assertThat(body.get(0).getPrefix().getWhitespace()).isEqualTo("\n    ");
                Cf.Tag output = (Cf.Tag) body.get(1);
                assertThat(output.getPrefix().getComments()).singleElement()
                  .satisfies(c -> assertThat(c.getSuffix()).isEqualTo("\n    "));
                List<Statement> text = output.getBody().getStatements();
                assertThat(((Cf.Text) text.get(0)).getText()).isEqualTo("Hi");
                assertThat(text.get(1).getPrefix().getWhitespace()).isEqualTo(" ");
                assertThat(cfif.getBody().getEnd().getWhitespace()).isEqualTo("\n");
                assertThat(cu.getEof().getComments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void poundsOnlyInterpolateInOutput() {
        rewriteRun(
          cfm(
            """
              <p style="color: #fff">#notInterpolated#</p>
              <cfoutput><p style="color: ##fff">#user.getName()# has #arrayLen(items)# items</p></cfoutput>
              """,
            spec -> spec.beforeRecipe(cu -> {
                assertThat(cu.getStatements().get(0)).isInstanceOf(Cf.Text.class);
                Cf.Tag output = (Cf.Tag) cu.getStatements().stream().filter(Cf.Tag.class::isInstance).findFirst().orElseThrow();
                assertThat(output.getBody().getStatements()).filteredOn(Cf.Interpolation.class::isInstance).hasSize(2);
            })
          )
        );
    }

    @Test
    void stringsInterpolateInAttributes() {
        rewriteRun(
          cfm(
            """
              <cfset greeting = "Hello, #user.name#! It's ""#dateFormat(now(), "yyyy")#"" and ##1">
              <cfset single = 'it''s'>
              """
          )
        );
    }

    @Test
    void varInCfset() {
        rewriteRun(
          cfc(
            """
              <cfcomponent output="false">
                  <cffunction name="go" access="public" returntype="string">
                      <cfargument name="who" type="string" required="true" />
                      <cfset var greeting = "Hello " & arguments.who>
                      <cfset var local.count = 0>
                      <cfreturn greeting />
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.MethodDeclaration function = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                List<Statement> body = function.getBody().getStatements();
                Cf.Tag var = (Cf.Tag) body.stream().filter(s -> s instanceof Cf.Tag && ((Cf.Tag) s).isNamed("cfset"))
                  .findFirst().orElseThrow();
                assertThat(var.getExpression()).isInstanceOf(J.VariableDeclarations.class);
            })
          )
        );
    }

    @Test
    void tagExpressionsEndAtTheTag() {
        rewriteRun(
          cfm(
            """
              <cfif (a > b) AND c LTE d>
                  <cfset total = a / b />
              </cfif>
              <cfreturn>
              """
          )
        );
    }

    @Test
    void cfscriptBlock() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  // a comment
                  x = 1;
                  if (x > 0) {
                      writeOutput("positive");
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag script = (Cf.Tag) cu.getStatements().get(0);
                assertThat(script.getBody().getStatements()).hasSize(2);
                assertThat(script.getBody().getStatements().get(0)).isInstanceOf(J.Assignment.class);
                assertThat(script.getBody().getStatements().get(1)).isInstanceOf(J.If.class);
            })
          )
        );
    }

    @Test
    void htmlVoidElementsAreText() {
        rewriteRun(
          cfm(
            """
              <form><input type="text" name="q"><br></form>
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(cu.getStatements()).singleElement().isInstanceOf(Cf.Text.class))
          )
        );
    }

    @Test
    void classicMacLineEndings() {
        rewriteRun(
          cfm("<cfscript>\r// a comment\rx = 1;\r</cfscript>\r")
        );
    }
}
