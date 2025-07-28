/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "DataFlowIssue", "GrUnnecessaryDefModifier"})
class VariableDeclarationsTest implements RewriteTest {

    @Test
    void varKeyword() {
        rewriteRun(
          groovy("var a = 1")
        );
    }

    @Test
    void finalKeyword() {
        rewriteRun(
          groovy("final a = 1")
        );
    }

    @Test
    void nestedGenerics() {
        rewriteRun(
          groovy("final map = new HashMap<String, List<String>>()")
        );
    }

    @Test
    void singleVariableDeclaration() {
        rewriteRun(
          groovy("Integer a = 1")
        );
    }

    @Test
    void singleVariableDeclarationStaticallyTyped() {
        rewriteRun(
          groovy(
            """
              int a = 1
              List<String> l
              """
          )
        );
    }

    @Test
    void wildcardWithUpperBound() {
        rewriteRun(
          groovy(
            """
              List<? extends String> l
              """,
            spec -> spec.beforeRecipe(cu -> {
                var varDecl = (J.VariableDeclarations) cu.getStatements().getFirst();
                assertThat(varDecl.getTypeExpression()).isInstanceOf(J.ParameterizedType.class);
                var typeExpression = requireNonNull(requireNonNull((J.ParameterizedType) varDecl.getTypeExpression())
                  .getTypeParameters()).getFirst();
                assertThat(typeExpression).isInstanceOf(J.Wildcard.class);
                assertThat(((J.Wildcard) typeExpression).getBound()).isEqualTo(J.Wildcard.Bound.Extends);
            })
          )
        );
    }

    @Test
    void wildcardWithLowerBound() {
        rewriteRun(
          groovy(
            """
              List<? super String> l
              """
          )
        );
    }

    @Test
    void diamondOperator() {
        rewriteRun(
          groovy("List<String> l = new ArrayList< /* */ >()")
        );
    }

    @Test
    void singleTypeMultipleVariableDeclaration() {
        rewriteRun(
          groovy("def a = 1, b = 1")
        );
    }

    @Test
    void multipleTypeMultipleVariableDeclaration() {
        rewriteRun(
          groovy("def a = 1, b = 's'")
        );
    }

    @Test
    void genericVariableDeclaration() {
        rewriteRun(
          groovy("def a = new HashMap<String, String>()")
        );
    }

    @Test
    void anonymousClass() {
        rewriteRun(
          groovy(
            """
              def a = new Object( ) {
                  def b = new Object() { }
              }
              """
          )
        );
    }

    @Test
    void annotatedField() {
        rewriteRun(
          groovy(
            """
              class Example {
                  @Deprecated
                  Object fred = new Object()
              }
              """,
            spec -> spec.beforeRecipe(
              cu -> {
                List<J.VariableDeclarations> variables = TreeVisitor.collect(new JavaVisitor<>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                        return SearchResult.found(multiVariable);
                    }
                }, cu, new ArrayList<>(), J.VariableDeclarations.class, v -> v);
                assertThat(variables.getFirst().getLeadingAnnotations()).hasSize(1);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4702")
    @Test
    void nestedTypeParameters() {
        rewriteRun(
          groovy(
                """
            class A {
                def map = new HashMap<String, List<String>>()
            }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4705")
    @Test
    void defAndExplicitReturnType() {
        rewriteRun(
          groovy(
            """
              def /*int*/ int one = 1
              def /*Object*/ Object two = 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4877")
    @Test
    void defVariableStartsWithDef() {
        rewriteRun(
          groovy(
            """
              def defaultPublicStaticFinal = 0
              """
          )
        );
    }
}
