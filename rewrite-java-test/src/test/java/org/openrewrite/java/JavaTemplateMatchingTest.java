/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class JavaTemplateMatchingTest {

    @Nested
    class ExpressionUsingVisitorFactory implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(toRecipe(() -> JavaTemplate.rewrite(
              J.Binary.class,
              visitor -> JavaTemplate.builder(visitor::getCursor, "#{any(String)}.length() == 0"),
              visitor -> JavaTemplate.builder(visitor::getCursor, "#{any(String)}.isEmpty()"),
              (matcher, template) -> {
                  return matcher.match().withTemplate(template, matcher.match().getCoordinates().replace(), matcher.parameter(0));
              }
            )));
        }

        @Test
        void unmatched() {
            rewriteRun(
              java("""
                public class Test {
                    boolean m(String s) {
                        return s.length() == 1;
                    }
                }
                """
              ));
        }

        @SuppressWarnings("ConstantValue")
        @Test
        void literalParameter() {
            rewriteRun(
              java("""
                public class Test {
                    boolean m() {
                        return "".length() == 0;
                    }
                }
                """, """
                public class Test {
                    boolean m() {
                        return "".isEmpty();
                    }
                }
                """));
        }

        @Test
        void variableParameter() {
            rewriteRun(
              java("""
                public class Test {
                    boolean m(String s) {
                        return s.length() == 0;
                    }
                }
                """, """
                public class Test {
                    boolean m(String s) {
                        return s.isEmpty();
                    }
                }
                """));
        }

        @Test
        void multiple() {
            rewriteRun(
              java("""
                public class Test {
                    boolean m(String s, String t) {
                        return (s.length() == 0 && t.length() == 0);
                    }
                }
                """, """
                public class Test {
                    boolean m(String s, String t) {
                        return (s.isEmpty() && t.isEmpty());
                    }
                }
                """));
        }
    }

    @Nested
    class StatementUsingVisitorFactory implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            assert true;
            spec.recipe(toRecipe(() -> JavaTemplate.rewrite(
              J.Assert.class,
              visitor -> JavaTemplate.builder(visitor::getCursor, "assert #{any(boolean)};"),
              visitor -> JavaTemplate.builder(visitor::getCursor, "if (!#{any(boolean)}) throw new AssertionError();"),
              (matcher, template) -> {
                  return matcher.match().withTemplate(template, matcher.match().getCoordinates().replace(), matcher.parameter(0));
              }
            )));
        }

        @Test
        void simple() {
            rewriteRun(
              java("""
                public class Test {
                    void m(String s) {
                        assert s.isEmpty();
                    }
                }
                """,
                """
                public class Test {
                    void m(String s) {
                        if (!s.isEmpty()) throw new AssertionError();
                    }
                }
                """
              ));
        }
    }

    @Nested
    class ExpressionUsingCustomVisitor implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                final JavaTemplate.Pattern<J.NewClass> before = JavaTemplate.builder(this::getCursor,
                    "new StringBuffer(#{any(int)})")
                  .buildPattern(J.NewClass.class);
                final JavaTemplate after = JavaTemplate.builder(this::getCursor,
                    "new StringBuilder(#{any(int)})")
                  .build();

                @Override
                public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                    newClass = super.visitNewClass(newClass, ctx);
                    JavaTemplate.Matcher<J.NewClass> matcher = before.matcher(newClass);
                    if (matcher.matches()) {
                        JavaCoordinates coordinates = newClass.getCoordinates().replace();
                        return newClass.withTemplate(after, coordinates, matcher.parameter(0));
                    }
                    return newClass;
                }
            }));
        }

        @Test
        void constructorCall() {
            rewriteRun(
              java("""
                public class Test {
                    CharSequence m() {
                        return new StringBuffer(10);
                    }
                }
                """, """
                public class Test {
                    CharSequence m() {
                        return new StringBuilder(10);
                    }
                }
                """));
        }
    }
}
