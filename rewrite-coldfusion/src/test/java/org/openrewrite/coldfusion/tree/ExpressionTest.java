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
import org.openrewrite.coldfusion.marker.FunctionKeyword;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.marker.SafeNavigation;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfm;

class ExpressionTest implements RewriteTest {

    /**
     * Parses {@code <cfscript>x = expression;</cfscript>} and hands over the expression.
     */
    private void assignedFrom(String expression, Consumer<Expression> check) {
        rewriteRun(
          cfm("<cfscript>\n    x = " + expression + ";\n</cfscript>\n",
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag script = (Cf.Tag) cu.getStatements().get(0);
                List<Statement> statements = script.getBody().getStatements();
                check.accept(((J.Assignment) statements.get(0)).getAssignment());
            }))
        );
    }

    @Test
    void wordOperators() {
        assignedFrom("a eq b and c is not d or not e", e -> {
            J.Binary or = (J.Binary) e;
            assertThat(or.getOperator()).isEqualTo(J.Binary.Type.Or);
            J.Binary and = (J.Binary) or.getLeft();
            assertThat(and.getOperator()).isEqualTo(J.Binary.Type.And);
            J.Binary isNot = (J.Binary) and.getRight();
            assertThat(isNot.getOperator()).isEqualTo(J.Binary.Type.NotEqual);
            assertThat(isNot.getMarkers().findFirst(OperatorSpelling.class)).get()
              .extracting(OperatorSpelling::getText).isEqualTo("is not");
            assertThat(or.getRight()).isInstanceOf(J.Unary.class);
        });
    }

    @Test
    void multiWordComparisons() {
        assignedFrom("a GREATER THAN OR EQUAL TO b AND c LESS  THAN d AND e DOES NOT CONTAIN f", e ->
          assertThat(((J.Binary) e).getOperator()).isEqualTo(J.Binary.Type.And));
    }

    @Test
    void notBindsLooserThanComparison() {
        assignedFrom("NOT a EQ b", e -> {
            J.Unary not = (J.Unary) e;
            assertThat(not.getExpression()).isInstanceOf(J.Binary.class);
        });
    }

    @Test
    void coldFusionOperators() {
        assignedFrom("a & b ^ 2 \\ c MOD d", e -> {
            Cf.Binary concat = (Cf.Binary) e;
            assertThat(concat.getOperator()).isEqualTo(Cf.Binary.Type.Concat);
            J.Binary mod = (J.Binary) concat.getRight();
            assertThat(mod.getOperator()).isEqualTo(J.Binary.Type.Modulo);
        });
    }

    @Test
    void ternaryAndElvis() {
        assignedFrom("a ? b : c ?: d", e -> assertThat(e).isInstanceOf(J.Ternary.class));
        assignedFrom("arguments.name ?: \"anonymous\"", e ->
          assertThat(((Cf.Binary) e).getOperator()).isEqualTo(Cf.Binary.Type.Elvis));
    }

    @Test
    void literals() {
        assignedFrom("[1, 2.5, \"three\", true, FALSE, null, [], {}]", e -> {
            J.NewArray array = (J.NewArray) e;
            assertThat(array.getTypeExpression()).isNull();
            assertThat(array.getInitializer()).hasSize(8);
            assertThat(array.getInitializer().get(6)).isInstanceOf(J.NewArray.class);
            assertThat(array.getInitializer().get(7)).isInstanceOf(Cf.StructLiteral.class);
        });
    }

    @Test
    void structLiterals() {
        assignedFrom("{ name: \"x\", \"key\" = 1, nested.key = 2, trailing: 3, }", e -> {
            Cf.StructLiteral struct = (Cf.StructLiteral) e;
            assertThat(struct.getEntries()).allMatch(Cf.KeyValue.class::isInstance).hasSize(4);
        });
        assignedFrom("[ a: 1, b: 2 ]", e -> assertThat(((Cf.StructLiteral) e).isOrdered()).isTrue());
        assignedFrom("[:]", e -> assertThat(((Cf.StructLiteral) e).isOrdered()).isTrue());
    }

    @Test
    void calls() {
        assignedFrom("obj.method(1, b = 2, c: 3, \"d\" = 4).chained()", e -> {
            J.MethodInvocation chained = (J.MethodInvocation) e;
            assertThat(chained.getSimpleName()).isEqualTo("chained");
            J.MethodInvocation method = (J.MethodInvocation) chained.getSelect();
            assertThat(method.getArguments()).hasSize(4);
            assertThat(method.getArguments().get(1)).isInstanceOf(Cf.KeyValue.class);
        });
    }

    @Test
    void chainsAcrossLines() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  print.line( "a" ).
                      toConsole();
                  qb.from( "users" )
                      .where( "active", 1 )
                      .get();
              </cfscript>
              """
          )
        );
    }

    @Test
    void safeNavigationAndStaticAccess() {
        assignedFrom("user?.address?.city", e ->
          assertThat(e.getMarkers().findFirst(SafeNavigation.class)).isPresent());
        assignedFrom("Util::format( x )", e -> assertThat(e).isInstanceOf(J.MethodInvocation.class));
    }

    @Test
    void indexAndInvocation() {
        assignedFrom("handlers[ key ]( event )", e -> assertThat(e).isInstanceOf(Cf.Invocation.class));
        assignedFrom("arguments[ \"name\" ]", e -> assertThat(e).isInstanceOf(J.ArrayAccess.class));
    }

    @Test
    void newComponents() {
        assignedFrom("new models.User( name = \"x\" )", e -> assertThat(e).isInstanceOf(J.NewClass.class));
        assignedFrom("new \"models.#type#\"()", e ->
          assertThat(((J.NewClass) e).getClazz()).isInstanceOf(Cf.TypeLiteral.class));
    }

    @Test
    void closuresAndArrows() {
        assignedFrom("function( required string a, b = 1 ) { return a & b; }", e ->
          assertThat(e.getMarkers().findFirst(FunctionKeyword.class)).isPresent());
        assignedFrom("( a, b ) => a + b", e -> assertThat(e).isInstanceOf(J.Lambda.class));
        assignedFrom("item => { return item.id; }", e -> assertThat(e).isInstanceOf(J.Lambda.class));
    }

    @Test
    void closureAttributes() {
        assignedFrom("function() localmode = \"classic\" { return 1; }", e -> {
            Cf.Closure closure = (Cf.Closure) e;
            assertThat(closure.getAttributes()).hasSize(1);
            assertThat(closure.getLambda().getBody()).isInstanceOf(J.Block.class);
        });
    }

    @Test
    void namedArgumentsWithoutCommas() {
        assignedFrom("cfparam(name=\"page\" default=1)", e ->
          assertThat(((J.MethodInvocation) e).getArguments()).hasSize(2));
    }

    @Test
    void poundsAroundExpressions() {
        assignedFrom("#a# + 1", e -> assertThat(((J.Binary) e).getLeft()).isInstanceOf(Cf.Interpolation.class));
    }

    @Test
    void compoundAssignments() {
        rewriteRun(
          cfm(
            """
              <cfscript>
                  a += 1;
                  b -= 1;
                  c *= 2;
                  d /= 2;
                  e %= 2;
                  f &= "suffix";
                  g++;
                  --h;
              </cfscript>
              """,
            spec -> spec.beforeRecipe(cu -> {
                Cf.Tag script = (Cf.Tag) cu.getStatements().get(0);
                assertThat(script.getBody().getStatements().get(5)).isInstanceOf(Cf.AssignmentOperation.class);
            })
          )
        );
    }
}
