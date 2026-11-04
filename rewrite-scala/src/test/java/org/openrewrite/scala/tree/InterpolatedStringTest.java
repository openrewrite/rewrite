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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class InterpolatedStringTest implements RewriteTest {

    @Test
    void simpleInterpolation() {
        rewriteRun(
          scala("val greeting = s\"Hello, I'm $name\"")
        );
    }

    @Test
    void multipleInterpolations() {
        rewriteRun(
          scala("val msg = s\"$name is $age\"")
        );
    }

    @Test
    void bracedInterpolation() {
        rewriteRun(
          scala("val x = s\"sum is ${a + b}!\"")
        );
    }

    @Test
    void bracedInterpolationWithInnerSpaces() {
        rewriteRun(
          scala("val x = s\"${ a + b }\"")
        );
    }

    @Test
    void fInterpolatorWithFormat() {
        rewriteRun(
          scala("val x = f\"${pi}%2.2f is pi\"")
        );
    }

    @Test
    void rawInterpolator() {
        rewriteRun(
          scala("val x = raw\"a\\nb $name\"")
        );
    }

    @Test
    void noInterpolations() {
        rewriteRun(
          scala("val x = s\"just text\"")
        );
    }

    @Test
    void memberAccessInterpolation() {
        rewriteRun(
          scala("val x = s\"name: ${person.name}\"")
        );
    }

    /**
     * Dotty reports each literal segment's span end as {@code start + decodedLength}, so a {@code $$}
     * escape (two source chars, one decoded char) leaves the span one char short. That dropped the
     * character before the next interpolation and shifted the {@code $} into the interpolation's
     * prefix, corrupting round-trips of strings that mix {@code $$} escapes with interpolations.
     */
    @Test
    void dollarEscapeFollowedByInterpolation() {
        rewriteRun(
          scala("val x = s\"\"\"$$('#x-$id')\"\"\"")
        );
    }

    /**
     * The embedded expressions must be first-class LST nodes, not text stuffed into a J.Identifier.
     * This is the assertion that fails against the old behavior (the whole {@code s"..."} was a
     * single J.Identifier whose simpleName held the raw source).
     */
    @Test
    void interpolationsAreStructured() {
        rewriteRun(
          spec -> spec.beforeRecipe(sources -> {
              List<S.InterpolatedString> strings = new ArrayList<>();
              List<String> interpolatedIdentifiers = new ArrayList<>();
              ScalaIsoVisitor<Integer> visitor = new ScalaIsoVisitor<Integer>() {
                  @Override
                  public S.InterpolatedString visitInterpolatedString(S.InterpolatedString is, Integer i) {
                      strings.add(is);
                      return super.visitInterpolatedString(is, i);
                  }

                  @Override
                  public S.Interpolation visitInterpolation(S.Interpolation in, Integer i) {
                      if (in.getExpression() instanceof J.Identifier) {
                          interpolatedIdentifiers.add(((J.Identifier) in.getExpression()).getSimpleName());
                      }
                      return super.visitInterpolation(in, i);
                  }
              };
              sources.forEach(source -> visitor.visit(source, 0));

              assertThat(strings).hasSize(1);
              S.InterpolatedString is = strings.get(0);
              assertThat(is.getInterpolator().getSimpleName()).isEqualTo("s");
              assertThat(is.getDelimiter()).isEqualTo("\"");
              assertThat(interpolatedIdentifiers).containsExactly("name", "age");
          }),
          scala("val msg = s\"$name is $age\"")
        );
    }
}
