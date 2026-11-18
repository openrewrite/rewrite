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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereScalaScopesThem() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  Scope scope = Scope.at(getCursor());
                  String visible = Arrays.stream(new String[]{"top", "a", "b", "x", "all", "inner", "i", "j"})
                    .filter(name -> scope.lookup(name).isPresent())
                    .collect(joining(" "));
                  String helper = scope.lookup("helper", Scope.Namespace.METHOD).isPresent() ? " helper" : "";
                  return SearchResult.found(m, visible + helper);
              }
          })),
          scala(
            """
              object O {
                val top = 1
                def f(p: (Int, Int), s: Any): Int = {
                  here()
                  val (a, b) = p
                  s match {
                    case x: String => here()
                    case all @ Some(inner) => here()
                  }
                  for (i <- List(1); j = i) yield here()
                  def helper(): Int = 1
                  here()
                }
                def here(): Int = 0
              }
              """,
            """
              object O {
                val top = 1
                def f(p: (Int, Int), s: Any): Int = {
                  /*~~(top helper)~~>*/here()
                  val (a, b) = p
                  s match {
                    case x: String => /*~~(top a b x helper)~~>*/here()
                    case all @ Some(inner) => /*~~(top a b all inner helper)~~>*/here()
                  }
                  for (i <- List(1); j = i) yield /*~~(top a b i j helper)~~>*/here()
                  def helper(): Int = 1
                  /*~~(top a b helper)~~>*/here()
                }
                def here(): Int = 0
              }
              """
          )
        );
    }
}
