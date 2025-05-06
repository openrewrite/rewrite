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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class AnnotationMatcherTest implements RewriteTest {

    @Test
    void matchAnnotation() {
        rewriteRun(
          kotlin(
            """
              @Deprecated("")
              class A
              """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    AnnotationMatcher matcher = new AnnotationMatcher("@kotlin.Deprecated");
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.Annotation visitAnnotation(J.Annotation annotation, AtomicBoolean atomicBoolean) {
                            if (matcher.matches(annotation)) {
                                found.set(true);
                            }
                            return super.visitAnnotation(annotation, atomicBoolean);
                        }
                    }.visit(cu, found);
                    assertThat(found.get()).isTrue();
                })
          )
        );
    }
}
