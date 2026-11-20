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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereGroovyScopesThem() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  Scope scope = Scope.at(getCursor());
                  String visible = Stream.of("top", "p", "a", "it", "x")
                    .filter(name -> scope.lookup(name).isPresent())
                    .collect(joining(" "));
                  return SearchResult.found(m, visible.isEmpty() ? "none" : visible);
              }
          })),
          groovy(
            """
              def top = 1
              def m(int p, List l) {
                  def a = p
                  l.each { here() }
                  l.each { x -> here() }
                  def none = { -> here() }
                  here()
              }
              def here() {}
              here()
              """,
            """
              def top = 1
              def m(int p, List l) {
                  def a = p
                  l.each { /*~~(p a it)~~>*/here() }
                  l.each { x -> /*~~(p a x)~~>*/here() }
                  def none = { -> /*~~(p a)~~>*/here() }
                  /*~~(p a)~~>*/here()
              }
              def here() {}
              /*~~(top)~~>*/here()
              """
          )
        );
    }
}
