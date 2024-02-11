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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AssertionsTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/30")
    @Test
    void isChanged() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                  J.VariableDeclarations.NamedVariable n = super.visitVariable(variable, ctx);
                  if ("a".equals(n.getSimpleName())) {
                      return n.withName(n.getName().withSimpleName("b").withType(null).withFieldType(null)).withType(null).withVariableType(null);
                  }
                  return n;
              }
          })),
          kotlin(
            """
            class A {
                val a = 1
            }
            """,
            """
            class A {
                val b = 1
            }
            """
          )
        );
    }

    @Test
    void invalidSyntax() {
        Optional<SourceFile> sf = KotlinParser.builder().build()
                .parse(
                    //language=none
                    "a++")
                .findFirst();
        assertThat(sf.isPresent()).isEqualTo(true);
        //noinspection OptionalGetWithoutIsPresent
        assertThat(sf.get()).isInstanceOf(ParseError.class);
    }
}
