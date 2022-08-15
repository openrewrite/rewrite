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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface NoValueOfOnStringTypeTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = NoValueOfOnStringType()

    @Test
    fun doNotChangeOnObject() = assertUnchanged(
        before = """
            class Test {
                static String method(Object obj) {
                    return String.valueOf(obj);
                }
            }
        """
    )

    @Test
    @Suppress(
        "UnnecessaryCallToStringValueOf",
        "UnusedAssignment",
        "StringConcatenationMissingWhitespace",
        "ImplicitArrayToString"
    )
    fun valueOfOnLiterals() = assertChanged(
        before = """
            class Test {
                static void method(char[] data) {
                    String str = String.valueOf("changeMe");
                    str = String.valueOf(0);
                    str = "changeMe" + String.valueOf(0);
                    str = String.valueOf(data);
                    str = "changeMe" + String.valueOf(data);
                    str = String.valueOf(data, 0, 0);
                    str = "doNotChangeMe" + String.valueOf(data, 0, 0);
                }
            }
        """,
        after = """
            class Test {
                static void method(char[] data) {
                    String str = "changeMe";
                    str = String.valueOf(0);
                    str = "changeMe" + 0;
                    str = String.valueOf(data);
                    str = "changeMe" + String.valueOf(data);
                    str = String.valueOf(data, 0, 0);
                    str = "doNotChangeMe" + String.valueOf(data, 0, 0);
                }
            }
        """
    )

    @Test
    @Suppress("UnnecessaryCallToStringValueOf")
    fun valueOfOnNonStringPrimitiveWithinBinaryConcatenation() = assertChanged(
        before = """
            class Test {
                static void count(int i) {
                    System.out.println("Count: " + String.valueOf(i));
                }
            }
        """,
        after = """
            class Test {
                static void count(int i) {
                    System.out.println("Count: " + i);
                }
            }
        """
    )

    @Test
    @Suppress("UnnecessaryCallToStringValueOf")
    fun valueOfOnNonStringPrimitiveWithinBinaryNotAString() = assertUnchanged(
        before = """
            class Test {
                static void count(int i) {
                    String fred = String.valueOf(i) + i;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1200")
    fun valueOfIsMethodInvocationPartOfBinary() = assertUnchanged(
        before = """
            class Test {
                static String method(Long id) {
                    return "example" + Test.method(String.valueOf(id));
                }

                static String method(String str) {
                    return str;
                }
            }
        """
    )

    @Test
    @Suppress("UnnecessaryCallToStringValueOf", "StringConcatenationMissingWhitespace")
    fun valueOfOnStandaloneNonStringPrimitive() = assertChanged(
        before = """
            class Test {
                static void method(int i) {
                    String str = String.valueOf(i) + "example";
                }
            }
        """,
        after = """
            class Test {
                static void method(int i) {
                    String str = i + "example";
                }
            }
        """
    )

    @Test
    fun concatenationResultingInNonString() = assertUnchanged(
        before = """
            class Test {
                static void method(int i) {
                    String str = i + String.valueOf(i);
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1200")
    @Suppress("IndexOfReplaceableByContains", "StatementWithEmptyBody")
    fun valueOfOnIntWithinBinaryComparison() = assertUnchanged(
        before = """
            class Test {
                static void method(String str, int i) {
                    if (str.indexOf(String.valueOf(i)) >= 0) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    @Suppress("UnnecessaryCallToStringValueOf")
    fun valueOfOnMethodInvocation() = assertChanged(
        before = """
            class Test {
                static void method1() {
                    String a = String.valueOf(method2());
                }

                static String method2() {
                    return "";
                }
            }
        """,
        after = """
            class Test {
                static void method1() {
                    String a = method2();
                }

                static String method2() {
                    return "";
                }
            }
        """
    )

}
