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
import org.openrewrite.coldfusion.ColdFusionParser;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NormalizeFormatTest implements RewriteTest {

    @Test
    void removedModifierLeavesItsSpaceWithTheFunction() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (!m.getModifiers().isEmpty()) {
                      // as a recipe removes public, which leaves the return type's space before it
                      m = m.withModifiers(emptyList());
                      return (J.MethodDeclaration) new NormalizeFormatVisitor<ExecutionContext>().visitNonNull(m, ctx, getCursor().getParentOrThrow());
                  }
                  return m;
              }
          })),
          cfc(
            """
              component {
                  public string function f() {
                      return "";
                  }
              }
              """,
            """
              component {
                   string function f() {
                      return "";
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration f = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                assertThat(f.getPrefix().getWhitespace()).isEqualTo("\n     ");
                assertThat(f.getReturnTypeExpression().getPrefix()).isEqualTo(Space.EMPTY);
            })
          )
        );
    }

    @Test
    void firstOperandsSpaceGoesToItsExpression() {
        Cf.CompilationUnit cu = (Cf.CompilationUnit) ColdFusionParser.builder().build()
          .parse("component { function f() { return a + b; } }").findFirst().orElseThrow();
        // as a recipe builds an expression, giving its first operand the space before it
        Cf.CompilationUnit built = (Cf.CompilationUnit) new ColdFusionIsoVisitor<Integer>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, Integer p) {
                return binary.withLeft(binary.getLeft().withPrefix(binary.getPrefix())).withPrefix(Space.EMPTY);
            }
        }.visitNonNull(cu, 0);
        Cf.CompilationUnit normalized = (Cf.CompilationUnit) new NormalizeFormatVisitor<Integer>().visitNonNull(built, 0);
        assertThat(normalized.printAll()).isEqualTo(cu.printAll());
        new ColdFusionIsoVisitor<Integer>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, Integer p) {
                assertThat(binary.getPrefix().getWhitespace()).isEqualTo(" ");
                assertThat(binary.getLeft().getPrefix()).isEqualTo(Space.EMPTY);
                return binary;
            }
        }.visit(normalized, 0);
    }

    @Test
    void parsedTreesStayAsTheyAre() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new NormalizeFormatVisitor<>())),
          cfc(
            """
              component extends="Base" {
                  property name="x";
                  public string function f(required string a) {
                      var local.b = a.trim() & "x";
                      return local.b;
                  }
              }
              """
          )
        );
    }
}
