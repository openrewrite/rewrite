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
                   { int n = /*~~(declared in MethodDeclaration)~~>*/p; this./*~~(declared in ClassDeclaration)~~>*/n = /*~~(declared in Block)~~>*/n; /*~~(declared in MethodDeclaration)~~>*/other./*~~(unbound)~~>*/n = /*~~(declared in Block)~~>*/n; Test./*~~(declared in ClassDeclaration)~~>*/n = /*~~(declared in Block)~~>*/n; }
                   return /*~~(unbound)~~>*/undefined;
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
