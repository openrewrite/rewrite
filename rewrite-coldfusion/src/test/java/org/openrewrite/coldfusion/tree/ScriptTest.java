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
import org.openrewrite.coldfusion.marker.AttributeComma;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.ReturnTypePosition;
import org.openrewrite.coldfusion.marker.VarKeyword;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;

class ScriptTest implements RewriteTest {

    private static List<Statement> script(Cf.CompilationUnit cu) {
        Cf.Tag script = (Cf.Tag) cu.getStatements().stream()
          .filter(s -> s instanceof Cf.Tag && ((Cf.Tag) s).isNamed("cfscript")).findFirst().orElseThrow();
        return script.getBody().getStatements();
    }

    @Test
    void semicolonsAreOptional() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  a = 1;
                  b = 2
                  c = 3 ;
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag script = (Cf.Tag) cu.getStatements().get(0);
                List<JRightPadded<Statement>> statements = script.getBody().getPadding().getStatements();
                assertThat(statements).hasSize(3);
                assertThat(statements.get(0).getMarkers().findFirst(Semicolon.class)).isPresent();
                assertThat(statements.get(1).getMarkers().findFirst(Semicolon.class)).isEmpty();
                assertThat(statements.get(2).getAfter().getWhitespace()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  if (a == 1) {
                      x = 1;
                  } else if (a == 2) x = 2;
                  else {
                      x = 3;
                  }
                  If (b) { y = 1; } ELSE { y = 2; }
                  if (c) { z = 1; } elseif (d) { z = 2; }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                J.If first = (J.If) statements.get(0);
                assertThat(first.getElsePart().getBody()).isInstanceOf(J.If.class);
                J.If second = (J.If) statements.get(1);
                assertThat(second.getMarkers().findFirst(KeywordSpelling.class)).get()
                  .extracting(KeywordSpelling::getText).isEqualTo("If");
                J.If third = (J.If) statements.get(2);
                J.If elseif = (J.If) third.getElsePart().getBody();
                assertThat(elseif.getPrefix().getWhitespace()).isEqualTo(" ");
                assertThat(elseif.getIfCondition().getPrefix().isEmpty()).isTrue();
            })
          )
        );
    }

    @Test
    void loops() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  for (var i = 1; i <= arrayLen(items); i++) {
                      total += items[i];
                  }
                  for (key in struct) writeOutput(key);
                  for (var item IN items) { }
                  for (;;) { break; }
                  while (more()) { continue; }
                  do { n--; } while (n > 0);
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                assertThat(statements.get(0)).isInstanceOf(J.ForLoop.class);
                assertThat(statements.get(1)).isInstanceOf(J.ForEachLoop.class);
                J.ForEachLoop forIn = (J.ForEachLoop) statements.get(2);
                assertThat(forIn.getControl().getMarkers().findFirst(KeywordSpelling.class)).isPresent();
                assertThat(statements.get(3)).isInstanceOf(J.ForLoop.class);
                assertThat(statements.get(4)).isInstanceOf(J.WhileLoop.class);
                assertThat(statements.get(5)).isInstanceOf(J.DoWhileLoop.class);
            })
          )
        );
    }

    @Test
    void switchCase() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  switch (lcase(action)) {
                      case "save":
                      case "update":
                          save();
                          break;
                      DEFAULT:
                          list();
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.Switch aSwitch = (J.Switch) script(cu).get(0);
                assertThat(aSwitch.getCases().getStatements()).hasSize(3);
            })
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  try {
                      risky();
                  } catch (database e) {
                      log(e);
                  } catch ("MyApp.NotFound" e) {
                      rethrow;
                  } catch (any var e) {
                      throw;
                  } finally {
                      cleanup();
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.Try aTry = (J.Try) script(cu).get(0);
                assertThat(aTry.getCatches()).hasSize(3);
                J.VariableDeclarations quoted = aTry.getCatches().get(1).getParameter().getTree();
                assertThat(quoted.getTypeExpression()).isInstanceOf(Cf.TypeLiteral.class);
                J.VariableDeclarations.NamedVariable var = aTry.getCatches().get(2).getParameter().getTree().getVariables().get(0);
                assertThat(var.getMarkers().findFirst(VarKeyword.class)).isPresent();
                assertThat(aTry.getFinally()).isNotNull();
            })
          )
        );
    }

    @Test
    void commentBetweenKeywordAndBlock() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  try // empty strings in queries
                  {
                      x = q[c][r];
                  }
                  catch (Any e)
                  {
                      x = "";
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(script(cu)).singleElement().isInstanceOf(J.Try.class))
          )
        );
    }

    @Test
    void varDeclarations() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  var a = 1;
                  var b;
                  var local.c = 2;
                  var sql &= " ORDER BY id";
                  while ((var line = reader.readLine()) != "") { }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                assertThat(statements.get(0)).isInstanceOf(J.VariableDeclarations.class);
                assertThat(statements.get(1)).isInstanceOf(J.VariableDeclarations.class);
                assertThat(statements.get(2)).isInstanceOf(J.Assignment.class);
                assertThat(statements.get(2).getMarkers().findFirst(VarKeyword.class)).isPresent();
            })
          )
        );
    }

    @Test
    void scriptTags() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  param name="url.page" default=1;
                  param rc.sort = "name";
                  include "header.cfm";
                  lock name="counter" type="exclusive" timeout=5 {
                      application.count++;
                  }
                  transaction {
                      save();
                  }
                  savecontent variable="html" {
                      writeOutput("<p>hi</p>");
                  }
                  abort;
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                assertThat(statements).allMatch(Cf.ScriptTag.class::isInstance);
                Cf.ScriptTag param = (Cf.ScriptTag) statements.get(0);
                assertThat(param.getAttributes()).hasSize(2);
                Cf.ScriptTag shorthand = (Cf.ScriptTag) statements.get(1);
                assertThat(shorthand.getExpression()).isInstanceOf(J.Assignment.class);
                Cf.ScriptTag lock = (Cf.ScriptTag) statements.get(3);
                assertThat(lock.getBody()).isNotNull();
            })
          )
        );
    }

    @Test
    void commasBetweenAScriptTagsAttributes() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  thread name="install" slug="cbauth", version="1.0.0" , action="run" {
                      install();
                  }
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.ScriptTag thread = (Cf.ScriptTag) script(cu).get(0);
                assertThat(thread.getAttributes()).extracting(a -> a.getName().getSimpleName())
                  .containsExactly("name", "slug", "version", "action");
                assertThat(thread.getAttributes()).extracting(a -> a.getMarkers().findFirst(AttributeComma.class)
                    .map(comma -> comma.getBefore().getWhitespace()).orElse(null))
                  .containsExactly(null, null, "", " ");
                assertThat(thread.getBody()).isNotNull();
            })
          )
        );
    }

    @Test
    void returnTypeBeforeAccessModifier() {
        rewriteRun(
          cfc(
            """
              component {
                  boolean public function isPackage(required string directory) {
                      return true;
                  }
                  string public static function name() {
                      return "x";
                  }
                  public numeric function count() {
                      return 1;
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> functions = cu.getClasses().get(0).getBody().getStatements();
                J.MethodDeclaration isPackage = (J.MethodDeclaration) functions.get(0);
                assertThat(isPackage.getReturnTypeExpression()).isInstanceOf(J.Identifier.class);
                assertThat(isPackage.getModifiers()).extracting(J.Modifier::getType).containsExactly(J.Modifier.Type.Public);
                assertThat(isPackage.getMarkers().findFirst(ReturnTypePosition.class).map(ReturnTypePosition::getPosition))
                  .contains(0);
                J.MethodDeclaration name = (J.MethodDeclaration) functions.get(1);
                assertThat(name.getModifiers()).hasSize(2);
                // a return type after the modifiers needs no marker
                J.MethodDeclaration count = (J.MethodDeclaration) functions.get(2);
                assertThat(count.getMarkers().findFirst(ReturnTypePosition.class)).isEmpty();
            })
          )
        );
    }

    @Test
    void tagWrittenAsCallWithBody() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  cfloop(array=items item="i" index="index") {
                      writeOutput(i);
                  }
                  cfparam(name="url.page", default=1);
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> statements = script(cu);
                Cf.TagCall loop = (Cf.TagCall) statements.get(0);
                assertThat(loop.getPrefix().getWhitespace()).isEqualTo("\n    ");
                assertThat(loop.getCall().getPrefix().isEmpty()).isTrue();
                assertThat(loop.getCall().getArguments()).hasSize(3);
                assertThat(loop.getBody().getStatements()).hasSize(1);
                assertThat(statements.get(1)).isInstanceOf(J.MethodInvocation.class);
            })
          )
        );
    }

    @Test
    void returnsAndThrows() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  function a() { return; }
                  function b() { return [ 1, 2 ]; }
                  function c() { throw "bad"; }
                  function d() { throw(message="bad", type="App"); }
                  function e() { throw message="bad" type="App"; }
              </cfscript>
              """
          )
        );
    }

    @Test
    void lucee() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  <!--- a tag comment in script --->
                  static {
                      COLORS = {};
                  }
                  try { connect(); } catch (any e) { retry; }
              </cfscript>
              """
          )
        );
    }
}
