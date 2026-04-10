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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Tests for GroovyTemplate when the method invocation being replaced has a
 * resolved void return type. In this case, the template generator must use
 * an initializer block wrapping instead of a class body field declaration,
 * because bare assignments like {@code version = expr;} are not valid at
 * Groovy class body level.
 */
class GroovyTemplateExplicitTypeArgParsingTest implements RewriteTest {

    @Test
    void groovyTemplateWithVoidMethodType() {
        // When the Groovy compiler resolves a void method, contextFreeTemplate must
        // use initializer block wrapping — bare assignments at class body level are
        // not valid Groovy syntax.
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                  if ("version".equals(m.getSimpleName()) && m.getArguments().size() == 1 &&
                      !(m.getArguments().get(0) instanceof J.Empty)) {
                      return GroovyTemplate.apply("version = #{any()}",
                              getCursor(), m.getCoordinates().replace(),
                              m.getArguments().get(0));
                  }
                  return m;
              }
          })),
          groovy(
            """
              class Config {
                  void version(String v) {}
                  void configure() {
                      version('1.0')
                  }
              }
              """,
            """
              class Config {
                  void version(String v) {}
                  void configure() {
                      version = '1.0'
                  }
              }
              """
          )
        );
    }
}
