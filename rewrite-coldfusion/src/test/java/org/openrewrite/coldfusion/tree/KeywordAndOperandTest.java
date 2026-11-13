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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfm;

/**
 * Words that are keywords in one place and names in another, and tokens whose meaning depends on
 * whether an operand came before them.
 */
class KeywordAndOperandTest implements RewriteTest {

    private static List<Statement> script(Cf.CompilationUnit cu) {
        Cf.Tag script = (Cf.Tag) cu.getStatements().stream()
          .filter(s -> s instanceof Cf.Tag && ((Cf.Tag) s).isNamed("cfscript")).findFirst().orElseThrow();
        return script.getBody().getStatements();
    }

    @Test
    void keywordsNamingVariables() {
        rewriteRun(
          cfm(
            """
              <cfif log.isInfoEnabled() AND NOT continue>
                  <cfreturn continue />
              </cfif>
              <cfset var null = "no">
              <cfset var catch = "">
              <cfset in[fields[i].name] = 1>
              <cfoutput>#replace(luceeCatchToString(catch), "<", "", "all")# #catch.message#</cfoutput>
              """
          )
        );
    }

    @Test
    void keywordsStillStartStatements() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  for (var key in items) {
                      if (key == "skip") continue;
                      if (key == "stop") {
                          break;
                      }
                  }
                  try { risky(); } catch (any e) { } finally { cleanup(); }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                assertThat(statements.get(0)).isInstanceOf(J.ForEachLoop.class);
                assertThat(statements.get(1)).isInstanceOf(J.Try.class);
            })
          )
        );
    }

    @Test
    void memberNamesStartingWithDigits() {
        rewriteRun(
          cfm("<cfoutput>#prc.statusReport.2FactorAuthEnabled#</cfoutput>")
        );
    }

    @Test
    void signsAfterOperandsAreBinary() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  x = a - -b
                    + 1;
                  y = -1;
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.Binary x = (J.Binary) ((J.Assignment) script(cu).get(0)).getAssignment();
                assertThat(x.getOperator()).isEqualTo(J.Binary.Type.Addition);
                J.Binary minus = (J.Binary) x.getLeft();
                assertThat(minus.getRight()).isInstanceOf(J.Unary.class);
            })
          )
        );
    }

    @Test
    void notAfterArithmeticNegatesItsOperand() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  x = a & !b EQ c;
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.Binary eq = (J.Binary) ((J.Assignment) script(cu).get(0)).getAssignment();
                assertThat(eq.getOperator()).isEqualTo(J.Binary.Type.Equal);
                Cf.Binary concat = (Cf.Binary) eq.getLeft();
                assertThat(concat.getRight()).isInstanceOf(J.Unary.class);
            })
          )
        );
    }

    @Test
    void callsAndIndexesContinueOnlyOnTheirLine() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  describe(title = "suite", body = function() {
                      it("works", function() { expect(true).toBeTrue(); });
                  });
                  result = isNull({"one" = 1}["two"]);
                  a = b
                  (c)
              </cfscript>
              <cfset d = e
                (f)>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                assertThat(statements.get(0)).isInstanceOf(J.MethodInvocation.class);
                assertThat(statements).hasSize(4);
                Cf.Tag set = (Cf.Tag) cu.getStatements().get(1);
                assertThat(((J.Assignment) set.getExpression()).getAssignment()).isInstanceOf(J.MethodInvocation.class);
            })
          )
        );
    }

    @Test
    void newTakesArgumentsOnItsLine() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  signer = new wheels.storage.S3Signer(
                      accessKeyId = config.accessKeyId,
                      region = config.region
                  );
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.NewClass newClass = (J.NewClass) ((J.Assignment) script(cu).get(0)).getAssignment();
                assertThat(newClass.getArguments()).hasSize(2);
            })
          )
        );
    }

    @Test
    void statementKeywordsNameVariablesInTagExpressions() {
        rewriteRun(
          cfm(
            """
              <cffunction name="f">
                  <cfset var Return = 1>
                  <CFRETURN Return>
              </cffunction>
              """
          )
        );
    }

    @Test
    void labelledContinue() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  loop array=items item="i" label="outer" {
                      loop array=others item="o" {
                          if (o == i) continue outer;
                      }
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.ScriptTag outer = (Cf.ScriptTag) script(cu).get(0);
                Cf.ScriptTag inner = (Cf.ScriptTag) outer.getBody().getStatements().get(0);
                J.If anIf = (J.If) inner.getBody().getStatements().get(0);
                J.Continue cont = (J.Continue) anIf.getThenPart();
                assertThat(cont.getLabel()).isNotNull();
                assertThat(cont.getLabel().getSimpleName()).isEqualTo("outer");
            })
          )
        );
    }

    @Test
    void elvisWithSpaceBetween() {
        rewriteRun(
          cfm("<cfoutput>#( rc.folder ? : \"\" )#</cfoutput>")
        );
    }

    @Test
    void arrayReturnTypes() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  public Element[] function getLinks() {
                      return [];
                  }
                  models.User[] function getUsers() {
                      return [];
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu ->
              assertThat(((J.MethodDeclaration) script(cu).get(0)).getReturnTypeExpression()).isInstanceOf(J.ArrayType.class))
          )
        );
    }
}
