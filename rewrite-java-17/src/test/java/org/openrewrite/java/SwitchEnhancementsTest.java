/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SwitchEnhancementsTest implements RewriteTest {
    @DocumentExample
    @Test
    void yieldReformated() {
        rewriteRun(
                spec -> spec.recipe(new AutoFormat()),
                java("""
                        class Test {
                            String yielded(int i) {
                                return switch (i) {
                                    default: yield"value";
                                };
                            }
                        }
                        """, """
                        class Test {
                            String yielded(int i) {
                                return switch (i) {
                                    default: yield "value";
                                };
                            }
                        }
                        """));
    }
}
