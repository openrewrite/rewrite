/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Issue("https://openjdk.org/jeps/513")
class JavaTemplateExplicitConstructorInvocationTest implements RewriteTest {

    private static RecipeSpec insertBeforeExplicitConstructorInvocation(RecipeSpec spec, boolean contextSensitive) {
        JavaTemplate.Builder builder = JavaTemplate.builder("String name1 = #{any(java.lang.String)};");
        if (contextSensitive) {
            builder.contextSensitive();
        }
        JavaTemplate template = builder.build();
        return spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                for (Statement statement : b.getStatements()) {
                    if (statement instanceof J.VariableDeclarations vd &&
                        "name1".equals(vd.getVariables().getFirst().getSimpleName())) {
                        return b;
                    }
                    if (statement instanceof J.MethodInvocation invocation && invocation.getSelect() == null &&
                        ("super".equals(invocation.getSimpleName()) || "this".equals(invocation.getSimpleName()))) {
                        return template.apply(getCursor(), invocation.getCoordinates().before(),
                          invocation.getArguments().getFirst());
                    }
                }
                return b;
            }
        }));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void beforeSuperAsFirstStatement(boolean contextSensitive) {
        rewriteRun(
          spec -> insertBeforeExplicitConstructorInvocation(spec, contextSensitive),
          java(
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      super(s.trim());
                  }
              }
              """,
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      String name1 = s.trim();
                      super(s.trim());
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void beforeSuperAfterOtherStatements(boolean contextSensitive) {
        rewriteRun(
          spec -> insertBeforeExplicitConstructorInvocation(spec, contextSensitive),
          java(
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      String validated = s.trim();
                      super(validated);
                  }
              }
              """,
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      String validated = s.trim();
                      String name1 = validated;
                      super(validated);
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void beforeThisAsFirstStatement(boolean contextSensitive) {
        rewriteRun(
          spec -> insertBeforeExplicitConstructorInvocation(spec, contextSensitive),
          java(
            """
              class A {
                  A(String s, int i) {
                  }
                  A(String s) {
                      this(s.trim(), 0);
                  }
              }
              """,
            """
              class A {
                  A(String s, int i) {
                  }
                  A(String s) {
                      String name1 = s.trim();
                      this(s.trim(), 0);
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void beforeThisAfterOtherStatements(boolean contextSensitive) {
        rewriteRun(
          spec -> insertBeforeExplicitConstructorInvocation(spec, contextSensitive),
          java(
            """
              class A {
                  A(String s, int i) {
                  }
                  A(String s) {
                      String validated = s.trim();
                      this(validated, 0);
                  }
              }
              """,
            """
              class A {
                  A(String s, int i) {
                  }
                  A(String s) {
                      String validated = s.trim();
                      String name1 = validated;
                      this(validated, 0);
                  }
              }
              """
          )
        );
    }
}
