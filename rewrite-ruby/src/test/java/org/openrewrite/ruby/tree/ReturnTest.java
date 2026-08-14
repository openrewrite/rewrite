/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;

public class ReturnTest implements RewriteTest {

    @Test
    void explicitReturn() {
        rewriteRun(
          ruby(
            """
              def sum(a, b)
                  return a + b
              end
              """
          )
        );
    }

    @Test
    void implicitReturn() {
        rewriteRun(
          ruby(
            """
              def sum(a, b)
                  a + b
              end
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicInteger counter = new AtomicInteger();
                new RubyIsoVisitor<AtomicInteger>() {
                    @Override
                    public J.Return visitReturn(J.Return aReturn, AtomicInteger p) {
                        aReturn.getMarkers().findFirst(ImplicitReturn.class)
                          .ifPresent(r -> counter.incrementAndGet());
                        return aReturn;
                    }
                }.visit(cu, counter);

                assertThat(counter.get()).isEqualTo(1);
            })
          )
        );
    }

    @Test
    void returnMoreThanOneValue() {
        rewriteRun(
          ruby(
            """
              def polar(x,y)
                  return Math.hypot(y,x), Math.atan2(y,x)
              end
              """
          )
        );
    }
}
