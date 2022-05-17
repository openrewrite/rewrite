/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.search

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("UnnecessaryLocalVariable")
interface UriCreatedWithHttpSchemeTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UriCreatedWithHttpScheme())
    }

    @Test
    fun findInsecureUri(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        String t = s;
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """, """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "https://test";
                        String t = s;
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun replaceIsABarrierGuard(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        s = s.replace("http://", "https://");
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Test
    @Disabled("MISSING: Assignment dominance")
    fun reassignmentBreaksDataFlowPath(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        s = "https://example.com";
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Test
    @Disabled("MISSING: Assignment dominance of conditional that will always evaluate to true")
    fun reassignmentInAlwaysEvaluatedPathBreaksDataFlowPath(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        if (true) {
                            s = "https://example.com";
                        }
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun reassignmentWithinBlockDoesNotBreakPath(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        if(System.currentTimeMillis() > 0) {
                            s = "https://example.com";
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """,
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "https://test";
                        if(System.currentTimeMillis() > 0) {
                            s = "https://example.com";
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun dataflowThroughTernaryOperator(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        String t = true ? s : null;
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """,
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "https://test";
                        String t = true ? s : null;
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun taintIsNotDataflow(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.io.File;
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test" + File.separator;
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun arbitraryMethodCallsAreNotDataflow(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.io.File;
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        String t = someMethod(s);
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }

                    String someMethod(String input) {
                        return null;
                    }
                }
            """
        )
    )

    @Test
    fun arbitraryMethodCallChainsAreNotDataFlow(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.io.File;
                import java.net.URI;
                import java.util.Locale;
                class Test {
                    void test() {
                        String s = "http://test";
                        String t = s.toLowerCase(Locale.ROOT);
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """
        )
    )

    @Test
    fun specialCaseToStringOnStringTypeIsDataFlow(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.io.File;
                import java.net.URI;
                import java.util.Locale;
                @SuppressWarnings("RedundantSuppression")
                class Test {
                    @SuppressWarnings("StringOperationCanBeSimplified")
                    void test() {
                        String s = "http://test";
                        String t = s.toString();
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """,
            """
                import java.io.File;
                import java.net.URI;
                import java.util.Locale;
                @SuppressWarnings("RedundantSuppression")
                class Test {
                    @SuppressWarnings("StringOperationCanBeSimplified")
                    void test() {
                        String s = "https://test";
                        String t = s.toString();
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(t));
                        } else {
                            System.out.println(URI.create(t));
                        }
                    }
                }
            """
        )
    )
}
