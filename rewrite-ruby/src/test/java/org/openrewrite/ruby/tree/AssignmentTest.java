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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.ruby.marker.SafeNavigation;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;

public class AssignmentTest implements RewriteTest {

    @Test
    void instanceAssignmentOperation() {
        rewriteRun(
          ruby(
            """
              def common_dir
                @common_dir ||= 1
              end
              """
          )
        );
    }

    @Test
    void localAssignment() {
        rewriteRun(
          ruby(
            """
              a = 1
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo, bar, baz", "foo,", "foo, *rest"})
    void multipleAssignment(String assign) {
        rewriteRun(
          ruby(
            """
              %s = [1, 2, 3]
              """.formatted(assign)
          )
        );
    }

    @Test
    void parallelAssignment() {
        rewriteRun(
          ruby(
            """
              a, b, c = 1, 2, 3
              """
          )
        );
    }

    @Test
    void starAssignment() {
        rewriteRun(
          ruby(
            """
              a, b, * = arr
              """
          )
        );
    }

    @Test
    void collectIntoSingleValueAssignment() {
        rewriteRun(
          ruby(
            """
              lhs = a, b, c
              """
          )
        );
    }

    @Test
    void assignMultiplication() {
        rewriteRun(
          ruby(
            """
              a = 1 * 2
              """
          )
        );
    }

    @Test
    void arrayAssignment() {
        rewriteRun(
          ruby(
            """
              a[1] = 1
              a[0,2] = ['A', 'B']
              """
          )
        );
    }

    @Test
    void attributeAssignment() {
        rewriteRun(
          ruby(
            """
              a.b = 1
              """
          )
        );
    }

    @Test
    void splatArrayAssignment() {
        rewriteRun(
          ruby(
            """
              a = [2, 3]
              b[*a]  = 1
              b[0, 1, *a]  = 1
              """
          )
        );
    }

    @Test
    void emptyArrayToAttribute() {
        rewriteRun(
          ruby(
            """
              spec.files = []
              """
          )
        );
    }

    @Test
    void global() {
        rewriteRun(
          ruby(
            """
              $a = 1
              """
          )
        );
    }

    @Test
    void safeNavigationAttributeAssignment() {
        rewriteRun(
          ruby(
            """
              @token_bucket&.addition = TokenBucket.new(tokens: [])
              """
          )
        );
    }

    @Test
    void bracketedArraysInParallelAssignment() {
        rewriteRun(
          ruby(
            """
              a, b = [1], [2]
              """
          )
        );
    }

    @Test
    void parenthesizedTargets() {
        rewriteRun(
          ruby(
            """
              (key, value) = line.split("=", 2)
              """
          )
        );
    }

    @Test
    void nestedDestructuring() {
        rewriteRun(
          ruby(
            """
              a, (b, c) = [1, [2, 3]]
              """
          )
        );
    }

    @Test
    void nestedDestructuringWithSplat() {
        rewriteRun(
          ruby(
            """
              a, (b, *c) = [1, [2, 3, 4]]
              """
          )
        );
    }

    @Test
    void nestedDestructuringOnly() {
        rewriteRun(
          ruby(
            """
              (a, b), c = [1, 2], 3
              """
          )
        );
    }

    @Test
    void deeplyNestedDestructuring() {
        rewriteRun(
          ruby(
            """
              a, (b, (c, d)) = [1, [2, [3, 4]]]
              """
          )
        );
    }

    /**
     * The attribute write and the operator writes reach different visitors, and each has to keep the
     * `&`.
     */
    @Test
    void safeNavigation() {
        rewriteRun(
          ruby(
            """
              user&.profile = profile
              config&.timeout ||= 30
              config&.retries &&= 3
              config&.count += 1
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicInteger counter = new AtomicInteger();
                new RubyIsoVisitor<AtomicInteger>() {
                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicInteger p) {
                        fieldAccess.getMarkers().findFirst(SafeNavigation.class)
                          .ifPresent(s -> counter.incrementAndGet());
                        return super.visitFieldAccess(fieldAccess, p);
                    }
                }.visit(cu, counter);

                assertThat(counter.get()).isEqualTo(4);
            })
          )
        );
    }
}
