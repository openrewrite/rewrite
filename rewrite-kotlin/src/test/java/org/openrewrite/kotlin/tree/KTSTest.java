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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlinScript;

class KTSTest implements RewriteTest {

    @Test
    void topLevelAssignmentExpression() {
        rewriteRun(
          kotlinScript(
                """
            var x = 5
            x += 1 /*C1*/
            """
          )
        );
    }

    @Test
    void topLevelFunctionCall() {
        rewriteRun(
          kotlinScript(
                """
            println("foo")
            """
          )
        );
    }

    @Test
    void topLevelForLoop() {
        rewriteRun(
          kotlinScript(
                """
            val items = listOf("foo", "bar", "buz")
            for (item in items) {
                println(item)
            }
            """
          )
        );
    }

    @Test
    void dslSample() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlinScript(
            //language=none
            """
            plugins {
                id("org.flywaydb.flyway") version "8.0.2"
            }
            repositories {
                mavenCentral()
                maven {
                    url = uri("https://maven.springframework.org/release")
                }
            }
            """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicInteger count = new AtomicInteger();
                    new KotlinIsoVisitor<AtomicInteger>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicInteger i) {
                            i.incrementAndGet();
                            return super.visitMethodInvocation(method, i);
                        }
                    }.visit(cu, count);
                    assertThat(count.get()).isEqualTo(7);
                }))
        );
    }
}
