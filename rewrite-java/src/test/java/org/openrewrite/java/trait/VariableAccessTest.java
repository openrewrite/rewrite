/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class VariableAccessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(markVariableAccesses(new VariableAccess.Matcher()));
    }

    @DocumentExample
    @Test
    void variableAccesses() {
        rewriteRun(
          java(
            """
              class Test {
                 int f;
                 int test(int p) {
                   int a = p;
                   p++;
                   p += 2;
                   (p) = p+1;
                   this.f = 3;
                   int negP = -p;
                   return a+1;
                 }
              }
              """,
            """
              class Test {
                 int f;
                 int test(int p) {
                   int a = /*~~(read p)~~>*/p;
                   /*~~(read+write p)~~>*/p++;
                   /*~~(read+write p)~~>*/p += 2;
                   (/*~~(write p)~~>*/p) = /*~~(read p)~~>*/p+1;
                   this./*~~(write f)~~>*/f = 3;
                   int negP = -/*~~(read p)~~>*/p;
                   return /*~~(read a)~~>*/a+1;
                 }
              }
              """,
            spec -> spec.afterRecipe(cu ->
                assertThat(new VariableAccess.Matcher().lower(cu)).hasSize(8))
          )
        );
    }

    @Test
    void patternVariableDeclarationIsNotAnAccess() {
        rewriteRun(
          java(
            """
              class Test {
                 int test(Object o) {
                   if (o instanceof String s) {
                     return s.length();
                   }
                   return 0;
                 }
              }
              """,
            """
              class Test {
                 int test(Object o) {
                   if (/*~~(read o)~~>*/o instanceof String s) {
                     return /*~~(read s)~~>*/s.length();
                   }
                   return 0;
                 }
              }
              """
          )
        );
    }

    @Test
    void packageOfAQualifiedTypeNameIsNotAnAccess() {
        rewriteRun(
          spec -> spec.recipe(markVariableAccesses(new VariableAccess.Matcher())),
          java(
            """
              class Test {
                 Object test() {
                   return java.util.Collections.emptyList();
                 }
              }
              """
          )
        );
    }

    @Test
    void accessResolvesToItsDeclarationByPosition() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(toRecipe(() -> new VariableAccess.Matcher().asVisitor(va -> SearchResult.found(va.getTree(),
              va.getBinding().map(b -> "declared in " + b.getScope().getTree().getClass().getSimpleName()).orElse("unbound"))))),
          java(
            """
              class Test {
                 static int n;
                 Object test(int p, Test other) {
                   { int n = p; this.n = n; other.n = n; Test.n = n; }
                   return undefined;
                 }
              }
              """,
            """
              class Test {
                 static int n;
                 Object test(int p, Test other) {
                   { int n = /*~~(declared in MethodDeclaration)~~>*/p; this./*~~(declared in ClassDeclaration)~~>*/n = /*~~(declared in Block)~~>*/n; /*~~(declared in MethodDeclaration)~~>*/other./*~~(declared in ClassDeclaration)~~>*/n = /*~~(declared in Block)~~>*/n; Test./*~~(declared in ClassDeclaration)~~>*/n = /*~~(declared in Block)~~>*/n; }
                   return /*~~(unbound)~~>*/undefined;
                 }
              }
              """
          )
        );
    }

    @Test
    void fieldSelectedFromSuperOrAnEnclosingInstanceResolvesToThatClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new VariableAccess.Matcher().asVisitor(va -> SearchResult.found(va.getTree(),
            va.getBinding().map(b -> b.getKind() + " of " + ((J.ClassDeclaration) b.getScope().getTree()).getSimpleName()).orElse("unbound"))))),
          java(
            """
              class Base {
                  int x;
                  int inherited;
              }
              class Sub extends Base {
                  int x;
                  void m() {
                      super.x = 1;
                      super.inherited = 2;
                  }
                  class Inner {
                      int x;
                      void m() {
                          Sub.this.x = 3;
                      }
                  }
              }
              """,
            """
              class Base {
                  int x;
                  int inherited;
              }
              class Sub extends Base {
                  int x;
                  void m() {
                      super./*~~(unbound)~~>*/x = 1;
                      super./*~~(INHERITED of Sub)~~>*/inherited = 2;
                  }
                  class Inner {
                      int x;
                      void m() {
                          Sub.this./*~~(MEMBER of Sub)~~>*/x = 3;
                      }
                  }
              }
              """
          )
        );
    }

    Recipe markVariableAccesses(VariableAccess.Matcher matcher) {
        return toRecipe(() -> matcher.asVisitor(va -> {
            String op = va.isReadAccess() && va.isWriteAccess() ? "read+write" :
              va.isReadAccess() ? "read" :
                va.isWriteAccess() ? "write" : "neither";
            return SearchResult.found(va.getTree(),
              op + " " + va.getTree().getSimpleName());
        }));
    }
}
