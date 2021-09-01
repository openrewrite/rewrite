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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FindDeprecatedMethodsTest : JavaRecipeTest {

    @Test
    fun ignoreDeprecationsInDeprecatedMethod(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindDeprecatedMethods(null, true),
        before = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        test(n + 1);
                    }
                }
            }
        """
    )

    @Test
    fun ignoreDeprecationsInDeprecatedClass(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindDeprecatedMethods(null, true),
        before = """
            @Deprecated
            class Test {
                @Deprecated
                void test(int n) {
                }
                
                Test() {
                    if(n == 1) {
                        test(n + 1);
                    }
                }
            }
        """
    )

    @Test
    fun findDeprecations(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindDeprecatedMethods(null, false),
        before = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        test(n + 1);
                    }
                }
            }
        """,
        after = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        /*~~>*/test(n + 1);
                    }
                }
            }
        """
    )

    @Test
    fun matchOnMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindDeprecatedMethods("java.lang.* *(..)", false),
        before = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        test(n + 1);
                    }
                }
            }
        """,
        after = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        /*~~>*/test(n + 1);
                    }
                }
            }
        """
    )

    @Test
    fun dontMatchWhenMethodDoesntMatch(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindDeprecatedMethods("org.junit.jupiter.api.* *(..)", false),
        before = """
            class Test {
                @Deprecated
                void test(int n) {
                    if(n == 1) {
                        test(n + 1);
                    }
                }
            }
        """
    )
}
