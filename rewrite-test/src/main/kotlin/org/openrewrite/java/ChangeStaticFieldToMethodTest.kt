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
package org.openrewrite.java

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

interface ChangeStaticFieldToMethodTest : JavaRecipeTest {
    override val recipe: ChangeStaticFieldToMethod
        get() = ChangeStaticFieldToMethod(
            "java.util.Collections",
            "EMPTY_LIST",
            "com.acme.Lists",
            null,
            "of"
        )

    companion object {
        @Language("java")
        private const val acmeLists = """
            package com.acme;

            import java.util.Collections;
            import java.util.List;

            class Lists {
                static <E> List<E> of() {
                    return Collections.emptyList();
                }
            }
        """

        @Language("java")
        private const val staticStringClass = """
            package com.acme;

            public class Example {
                public static final String EXAMPLE = "example";
            }
        """
    }

    @Test
    @Suppress("unchecked")
    fun migratesQualifiedField() = assertChanged(
        dependsOn = arrayOf(acmeLists),
        before = """
            import java.util.Collections;
            import java.util.List;

            class A {
                static List<String> empty() {
                    return Collections.EMPTY_LIST;
                }
            }
        """,
        after = """
            import com.acme.Lists;

            import java.util.List;

            class A {
                static List<String> empty() {
                    return Lists.of();
                }
            }
        """
    )

    @Test
    fun migratesStaticImportedField() = assertChanged(
        dependsOn = arrayOf(acmeLists),
        before = """
            import static java.util.Collections.EMPTY_LIST;

            class A {
                static Object empty() {
                    return EMPTY_LIST;
                }
            }
        """,
        after = """
            import com.acme.Lists;

            class A {
                static Object empty() {
                    return Lists.of();
                }
            }
        """
    )

    @Test
    fun migratesFullyQualifiedField() = assertChanged(
        dependsOn = arrayOf(acmeLists),
        before = """
            class A {
                static Object empty() {
                    return java.util.Collections.EMPTY_LIST;
                }
            }
        """,
        after = """
            import com.acme.Lists;

            class A {
                static Object empty() {
                    return Lists.of();
                }
            }
        """
    )

    @Test
    fun migratesFieldInitializer() = assertChanged(
        dependsOn = arrayOf(acmeLists),
        before = """
            import java.util.Collections;

            class A {
                private final Object collection = Collections.EMPTY_LIST;
            }
        """,
        after = """
            import com.acme.Lists;

            class A {
                private final Object collection = Lists.of();
            }
        """
    )

    @Test
    fun ignoresUnrelatedFields() = assertUnchanged(
        before = """
            import java.util.Collections;

            class A {
                static Object EMPTY_LIST = null;

                static Object empty1() {
                    return A.EMPTY_LIST;
                }

                static Object empty2() {
                    return EMPTY_LIST;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1156")
    fun migratesToJavaLangClass() = assertChanged(
        recipe = ChangeStaticFieldToMethod(
            "com.acme.Example",
            "EXAMPLE",
            "java.lang.System",
            null,
            "lineSeparator"
        ),
        dependsOn = arrayOf(staticStringClass),
        before = """
            package example;

            import com.acme.Example;

            class A {
                static String lineSeparator() {
                    return Example.EXAMPLE;
                }
            }
        """,
        after = """
            package example;

            class A {
                static String lineSeparator() {
                    return System.lineSeparator();
                }
            }
        """
    )

    @Test
    fun leavesOwnerAlone() = assertUnchanged(
        recipe = ChangeStaticFieldToMethod(
            "com.example.Test",
            "EXAMPLE",
            "com.doesntmatter.Foo",
            null,
            "BAR"),
        before = """
            package com.example;
            
            class Test {
                public static Object EXAMPLE = null;
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1626")
    fun constantToMethodOnStaticTarget() = assertChanged(
        recipe = ChangeStaticFieldToMethod(
            "constants.Constants",
            "SUCCESS_CODE",
            "io.netty.handler.codec.http.HttpResponseStatus",
            "OK",
            "codeAsText"),

        dependsOn = arrayOf(
            """
                package constants;

                public class Constants {
                    public static final String SUCCESS_CODE = "200";
                }
            """,
            """
                package io.netty.handler.codec.http;

                public class HttpResponseStatus {
                    public static final HttpResponseStatus OK = new HttpResponseStatus(200);

                    private final int code;
                    private HttpResponseStatus(int code) {
                        this.code = code;
                    }
                    
                    String codeAsText() {
                        return String.valueOf(code);
                    }
                }

            """
        ),
        before = """
            package com.example;
            
            import constants.Constants;
            
            class Test {
                public static String testMe() {
                    return Constants.SUCCESS_CODE;
                }
            }
        """,
        after = """
            package com.example;
            
            import io.netty.handler.codec.http.HttpResponseStatus;
            
            class Test {
                public static String testMe() {
                    return HttpResponseStatus.OK.codeAsText();
                }
            }
        """
    )
}
