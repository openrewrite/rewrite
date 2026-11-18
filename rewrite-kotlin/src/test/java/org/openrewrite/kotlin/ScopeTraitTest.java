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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.trait.VariableAccess;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereKotlinScopesThem() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> {
              String visible = Arrays.stream(new String[]{"top", "prop", "a", "b", "w", "it"})
                .filter(name -> scope.lookup(name).isPresent())
                .collect(joining(" "));
              return visible.isEmpty() ? "none" : visible;
          })),
          kotlin(
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> here()
                      }
                      here()
                      listOf(1).forEach { here() }
                  }
              }
              fun here() {}
              """,
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> /*~~(top prop a b w)~~>*/here()
                      }
                      /*~~(top prop a b)~~>*/here()
                      listOf(1).forEach { /*~~(top prop a b it)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void namedArgumentIsNotAVariableAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new VariableAccess.Matcher()
            .asVisitor(access -> SearchResult.found(access.getTree())))),
          kotlin(
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = local)
              }
              """,
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = /*~~>*/local)
              }
              """
          )
        );
    }

    @Test
    void freeNameAvoidsALaterDestructuredName() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> "a=" + scope.freeName("a", n -> false) + ", c=" + scope.freeName("c", n -> false))),
          kotlin(
            """
              fun m(pair: Pair<Int, Int>) {
                  here()
                  val (a, b) = pair
              }
              fun here() {}
              """,
            """
              fun m(pair: Pair<Int, Int>) {
                  /*~~(a=a1, c=c)~~>*/here()
                  val (a, b) = pair
              }
              fun here() {}
              """
          )
        );
    }

    private static Recipe atHere(Function<Scope, String> question) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return "here".equals(m.getSimpleName()) ? SearchResult.found(m, question.apply(Scope.at(getCursor()))) : m;
            }
        });
    }
}
