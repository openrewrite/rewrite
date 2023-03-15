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
package org.openrewrite.java.trait.variable;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class LocalVariableDeclTest implements RewriteTest {

    static Supplier<TreeVisitor<?, ExecutionContext>> visitVariable(
      BiFunction<J.VariableDeclarations.NamedVariable, Cursor, J.VariableDeclarations.NamedVariable> visitor
    ) {
        return () -> new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(
              J.VariableDeclarations.NamedVariable variable,
              ExecutionContext executionContext
            ) {
                return super.visitVariable(visitor.apply(variable, getCursor()), executionContext);
            }
        };
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(visitVariable((variable, cursor) -> LocalVariableDecl.viewOf(cursor).map(localVariableDecl -> {
            assertNotNull(localVariableDecl.getCallable(), "LocalVariableDecl callable is null");
            return SearchResult.found(variable);
        }).orSuccess(variable))));
    }

    @Test
    void testVariableVisit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(visitVariable((variable, cursor) -> {
                LocalVariableDecl p = LocalVariableDecl.viewOf(cursor).orSuccess(TraitErrors::doThrow);
                assertEquals("i", p.getName(), "LocalVariableDecl name is incorrect");
                assertEquals("test", p.getCallable().getName(), "Parameter callable name is incorrect");
                return SearchResult.found(variable);
            }
          ))),
          java(
            "class Test { void test() { int i = 0; } }",
            "class Test { void test() { int /*~~>*/i = 0; } }"
          )
        );
    }

    @Test
    @SuppressWarnings("ClassInitializerMayBeStatic")
    void testVariableInInitBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(visitVariable((variable, cursor) -> {
                LocalVariableDecl p = LocalVariableDecl.viewOf(cursor).orSuccess(TraitErrors::doThrow);
                assertEquals("i", p.getName(), "LocalVariableDecl name is incorrect");
                assertEquals("<obinit>", p.getCallable().getName(), "Parameter callable name is incorrect");
                return SearchResult.found(variable);
            }
          ))),
          java(
            "class Test { { int i = 0; } }",
            "class Test { { int /*~~>*/i = 0; } }"
          )
        );
    }

    @Test
    void testVariableInStaticInitBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(visitVariable((variable, cursor) -> {
                LocalVariableDecl p = LocalVariableDecl.viewOf(cursor).orSuccess(TraitErrors::doThrow);
                assertEquals("i", p.getName(), "LocalVariableDecl name is incorrect");
                assertEquals("<clinit>", p.getCallable().getName(), "Parameter callable name is incorrect");
                return SearchResult.found(variable);
            }
          ))),
          java(
            "class Test { static { int i = 0; } }",
            "class Test { static { int /*~~>*/i = 0; } }"
          )
        );
    }

    @Test
    void doesNotFindStaticFields() {
        rewriteRun(
          java(
            "class Test { static int i = 0; }"
          )
        );
    }

    @Test
    void doesNotFindParameters() {
        rewriteRun(
          java(
            "abstract class Test { abstract void test(int i); }"
          )
        );
    }

    @Test
    void doesNotFindStaticFieldsInAnonymousInnerClasses() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      new Object() {
                          static int i = 0;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotFindStaticFieldsInInnerClasses() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      class Potato {
                          static int i = 0;
                      }
                  }
              }
              """
          )
        );
    }
}
