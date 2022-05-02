/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface ReplaceDuplicateStringLiteralsTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = ReplaceDuplicateStringLiterals(false)

    override val parser: JavaParser
        get() {
            val jp = JavaParser.fromJavaVersion().build()
            jp.setSourceSet("main")
            return jp
        }

    val testParser: JavaParser
        get() {
            val jp = JavaParser.fromJavaVersion().build()
            jp.setSourceSet("test")
            return jp
        }

    @Test
    fun doesNotMeetCharacterLimit() = assertUnchanged(
        before = """
            class A {
                final String val1 = "val";
                final String val2 = "val";
                final String val3 = "val";
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1740")
    @Test
    fun doesNotApplyToTest() = assertUnchanged(
        testParser,
        before = """
            class A {
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1740")
    @Test
    fun testSourcesEnabled() = assertChanged(
        testParser,
        recipe = ReplaceDuplicateStringLiterals(true),
        before = """
            class A {
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
            }
        """,
        after = """
            class A {
                private static final String VALUE = "value";
                final String val1 = VALUE;
                final String val2 = VALUE;
                final String val3 = VALUE;
            }
        """
    )

    @Test
    fun doNotChangeLiteralsInAnnotations() = assertUnchanged(
        dependsOn = arrayOf("""
            public @interface Example {
                String value() default "";
            }
        """),
        before = """
            class A {
                @Example(value = "value")
                void method1() {}
                @Example(value = "value")
                void method2() {}
                @Example(value = "value")
                void method3() {}
            }
        """
    )

    @Test
    fun replaceRedundantFinalStrings() = assertChanged(
        before = """
            package org.foo;
            class A {
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
            }
        """,
        after = """
            package org.foo;
            class A {
                private static final String VALUE = "value";
                final String val1 = VALUE;
                final String val2 = VALUE;
                final String val3 = VALUE;
            }
        """
    )

    @Test
    fun replaceRedundantLiteralInMethodInvocation() = assertChanged(
        before = """
            class A {
                String method(String val) {
                    return null;
                }
                String val1 = method("value");
                String val2 = method("value");
                String val3 = method("value");
            }
        """,
        after = """
            class A {
                private static final String VALUE = "value";
                String method(String val) {
                    return null;
                }
                String val1 = method(VALUE);
                String val2 = method(VALUE);
                String val3 = method(VALUE);
            }
        """
    )

    @Test
    fun replaceRedundantLiteralsInNewClass() = assertChanged(
        before = """
            class A {
                void method() {
                    B b1 = new B("value");
                    B b2 = new B("value");
                    B b3 = new B("value");
                }
                private static class B {
                    B(String val) {
                    }
                }
            }
        """,
        after = """
            class A {
                private static final String VALUE = "value";
                void method() {
                    B b1 = new B(VALUE);
                    B b2 = new B(VALUE);
                    B b3 = new B(VALUE);
                }
                private static class B {
                    B(String val) {
                    }
                }
            }
        """
    )

    @Test
    fun multipleRedundantValues() = assertChanged(
        before = """
            class A {
                final String a1 = "value a";
                final String a2 = "value a";
                final String a3 = "value a";
                final String b1 = "value b";
                final String b2 = "value b";
                final String b3 = "value b";
            }
        """,
        after = """
            class A {
                private static final String VALUE_A = "value a";
                private static final String VALUE_B = "value b";
                final String a1 = VALUE_A;
                final String a2 = VALUE_A;
                final String a3 = VALUE_A;
                final String b1 = VALUE_B;
                final String b2 = VALUE_B;
                final String b3 = VALUE_B;
            }
        """
    )

    @Test
    fun transformStringValue() = assertChanged(
        before = """
            class A {
                final String val1 = "An example,, of a :: String with `` special __ characters.";
                final String val2 = "An example,, of a :: String with `` special __ characters.";
                final String val3 = "An example,, of a :: String with `` special __ characters.";
            }
        """,
        after = """
            class A {
                private static final String AN_EXAMPLE_OF_A_STRING_WITH_SPECIAL_CHARACTERS = "An example,, of a :: String with `` special __ characters.";
                final String val1 = AN_EXAMPLE_OF_A_STRING_WITH_SPECIAL_CHARACTERS;
                final String val2 = AN_EXAMPLE_OF_A_STRING_WITH_SPECIAL_CHARACTERS;
                final String val3 = AN_EXAMPLE_OF_A_STRING_WITH_SPECIAL_CHARACTERS;
            }
        """
    )

    @Test
    fun constantAlreadyExists() = assertChanged(
        before = """
            class A {
                private static final String CONSTANT = "value";
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
            }
        """,
        after = """
            class A {
                private static final String CONSTANT = "value";
                final String val1 = CONSTANT;
                final String val2 = CONSTANT;
                final String val3 = CONSTANT;
            }
        """
    )

    @Test
    fun constantExistsWithInnerClass() = assertChanged(
        before = """
            class A {
                private static final String CONSTANT = "value";
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
                
                private static class B {
                    // Do not change inner class value.
                    private static final String CONSTANT = "value";
                }
            }
        """,
        after = """
            class A {
                private static final String CONSTANT = "value";
                final String val1 = CONSTANT;
                final String val2 = CONSTANT;
                final String val3 = CONSTANT;
                
                private static class B {
                    // Do not change inner class value.
                    private static final String CONSTANT = "value";
                }
            }
        """
    )

    @Test
    fun preventNamespaceShadowingWithNonStringConstant() = assertChanged(
        before = """
            package org.foo;
            class A {
                private static final int VALUE = 1;
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
            }
        """,
        after = """
            package org.foo;
            class A {
                private static final String VALUE_1 = "value";
                private static final int VALUE = 1;
                final String val1 = VALUE_1;
                final String val2 = VALUE_1;
                final String val3 = VALUE_1;
            }
        """
    )

    @Test
    fun preventNamespaceShadowingOnExistingConstant() = assertChanged(
        before = """
            class A {
                // Change field name to prevent potential namespace conflicts.
                private static final String VALUE = "value";
                void newScope() {
                    final String VALUE = "name already exists";
                    final String val1 = "value";
                    final String val2 = "value";
                    final String val3 = "value";
                }
                void method() {
                    // Change existing method reference.
                    String valueRef = VALUE;
                }
                private static class B {
                    // Change existing inner class reference.
                    String innerClass = VALUE;
                }
            }
        """,
        after = """
            class A {
                // Change field name to prevent potential namespace conflicts.
                private static final String VALUE_1 = "value";
                void newScope() {
                    final String VALUE = "name already exists";
                    final String val1 = VALUE_1;
                    final String val2 = VALUE_1;
                    final String val3 = VALUE_1;
                }
                void method() {
                    // Change existing method reference.
                    String valueRef = VALUE_1;
                }
                private static class B {
                    // Change existing inner class reference.
                    String innerClass = VALUE_1;
                }
            }
        """
    )

    @Test
    fun preventNamespaceShadowingOnNewConstant() = assertChanged(
        before = """
            class A {
                final String val1 = "value";
                final String val2 = "value";
                final String val3 = "value";
                final String VALUE = "name space conflict";
            }
        """,
        after = """
            class A {
                private static final String VALUE_1 = "value";
                final String val1 = VALUE_1;
                final String val2 = VALUE_1;
                final String val3 = VALUE_1;
                final String VALUE = "name space conflict";
            }
        """
    )

    @Test
    fun multiVariableDeclaration() = assertChanged(
        before = """
            class A {
                final String val1 = "value", val2 = "value", diff = "here";
                final String val3 = "value";
                final String VALUE = "name space conflict";
            }
        """,
        after = """
            class A {
                private static final String VALUE_1 = "value";
                final String val1 = VALUE_1, val2 = VALUE_1, diff = "here";
                final String val3 = VALUE_1;
                final String VALUE = "name space conflict";
            }
        """
    )

    @Test
    fun replaceMixedRedundantLiterals() = assertChanged(
        before = """
            class A {
                final String val1 = "value";
                void methodA() {
                    methodB("value");
                }
                void methodB(String val) {
                    B b = new B("value");
                }
                private static class B {
                    B(String val) {
                    }
                }
            }
        """,
        after = """
            class A {
                private static final String VALUE = "value";
                final String val1 = VALUE;
                void methodA() {
                    methodB(VALUE);
                }
                void methodB(String val) {
                    B b = new B(VALUE);
                }
                private static class B {
                    B(String val) {
                    }
                }
            }
        """
    )
}
