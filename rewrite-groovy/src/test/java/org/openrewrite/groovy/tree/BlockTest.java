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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GrUnnecessarySemicolon")
class BlockTest implements RewriteTest {

    @Test
    void block() {
        rewriteRun(
          groovy(
            """
              implementation ('org.thymeleaf:thymeleaf-spring4:3.0.6.RELEASE') {
                  force = true;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9486")
    @ParameterizedTest
    @ValueSource(strings = {
      "if (true) { println(1);; }",
      "if (true) { println(1); ; }",
      "if (true) { println(1);;; }",
      "if (true) { println(1);; println(2) }",
      "if (true) { ; }",
      "if (true) { ;println(1) }",
      "if (true) { println(1) /* c */ ; }",
      "if (true) {\n    println(1);\n    ;\n}",
      "def c = { println(1);; }",
      "class A { void m() { int i = 1;; } }",
      "def a = 1;;",
      "def a = 1;; def b = 2",
    })
    void emptyStatement(String source) {
        rewriteRun(
          groovy(source)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9486")
    @Test
    void emptyStatementIsJEmpty() {
        rewriteRun(
          groovy(
            """
              if (true) { println(1);; }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(new GroovyIsoVisitor<AtomicInteger>() {
                @Override
                public J.Empty visitEmpty(J.Empty empty, AtomicInteger empties) {
                    empties.incrementAndGet();
                    return super.visitEmpty(empty, empties);
                }
            }.reduce(cu, new AtomicInteger())).hasValue(1))
          )
        );
    }
}
