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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class UnimportedReferencesTest {

    private static List<String> unimported(String... sources) {
        return JavaParser.fromJavaVersion().build().parse(sources)
          .map(J.CompilationUnit.class::cast)
          .flatMap(cu -> UnimportedReferences.of(cu).stream())
          .collect(toList());
    }

    @Test
    void importedStaticAndInheritedMembers() {
        assertThat(unimported(
          """
            package a;

            import java.util.List;
            import java.util.function.*;

            import static java.util.Collections.emptyList;
            import static java.util.Objects.*;

            class A extends B implements C {
                List<String> list = emptyList();
                Function<String, String> f = s -> requireNonNull(s);

                void m() {
                    inherited();
                    int x = CONSTANT + FROM_INTERFACE;
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            inherited();
                        }
                    };
                }

                static class Nested {
                    void n() {
                        inherited();
                    }
                }
            }
            """,
          """
            package a;

            class B {
                static final int CONSTANT = 1;

                static void inherited() {
                }
            }
            """,
          """
            package a;

            interface C {
                int FROM_INTERFACE = 2;
            }
            """
        )).isEmpty();
    }

    @Test
    void enumConstantsInCasesAndAnnotations() {
        assertThat(unimported(
          """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            import static java.lang.annotation.RetentionPolicy.RUNTIME;

            @Retention(RUNTIME)
            @interface Marked {
                RetentionPolicy value() default RetentionPolicy.CLASS;
            }

            class A {
                enum Color { RED, GREEN }

                @Marked(value = RetentionPolicy.SOURCE)
                String name(Color color) {
                    switch (color) {
                        case RED:
                            return "red";
                        default:
                            return Color.GREEN.name();
                    }
                }
            }
            """
        )).isEmpty();
    }

    @Test
    void javaLangSamePackageAndQualifiedNames() {
        assertThat(unimported(
          """
            package a;

            class A {
                String s = String.valueOf(Integer.MAX_VALUE);
                java.util.Map.Entry<String, B> entry;
                java.util.List<java.util.Map<String, Object>> nested;
            }
            """,
          """
            package a;

            class B {
            }
            """
        )).isEmpty();
    }
}
