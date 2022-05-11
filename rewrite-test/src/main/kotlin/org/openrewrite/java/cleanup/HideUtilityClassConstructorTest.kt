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
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.HideUtilityClassConstructorStyle
import org.openrewrite.style.NamedStyles

interface HideUtilityClassConstructorTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = HideUtilityClassConstructor()

    @Issue("https://github.com/openrewrite/rewrite/issues/1780")
    @Test
    fun doNotAddConstructorToInterface(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public interface A {
                public static final String utility = "";
            }
        """
    )

    /**
     * Should be a utility class since all methods are static, but class has public constructor
     */
    @Test
    fun changePublicConstructorToPrivate(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public A() {
                }

                public static void utility() {
                }
            }
        """,
        after = """
            public class A {
                private A() {
                }

                public static void utility() {
                }
            }
        """
    )

    @Test
    fun changePackagePrivateConstructorToPrivate(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                A() {
                }

                public static void utility() {
                }
            }
        """,
        after = """
            public class A {
                private A() {
                }

                public static void utility() {
                }
            }
        """
    )

    @Test
    fun identifyUtilityClassesWithProtectedConstructor(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                protected A() {
                }

                public static void utility() {
                }
            }
        """
    )

    @Test
    fun changeUtilityClassesWithMixedExposedConstructors(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                protected A() {
                }

                public A(String a) {
                }

                A(String a, String b) {
                }

                private A(String a, String b, String c) {
                }

                public static void utility() {
                }
            }
        """,
        after = """
            public class A {
                protected A() {
                }

                private A(String a) {
                }

                private A(String a, String b) {
                }

                private A(String a, String b, String c) {
                }

                public static void utility() {
                }
            }
        """
    )

    /**
     * Should be a utility class since all methods are static, but class has no constructor (default/package-private)
     */
    @Test
    fun addPrivateConstructorWhenOnlyDefaultConstructor(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Math {
                public static final int TWO = 2;

                public static int addTwo(int a) {
                    return a + TWO;
                }
            }
        """,
        after = """
            public class Math {
                public static final int TWO = 2;

                public static int addTwo(int a) {
                    return a + TWO;
                }

                private Math() {
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/538")
    fun ignoreClassesWithMainMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package a;

            public class A {
                public static void main(String[] args) {
                    // SpringApplication.run(A.class, args);
                }
            }
        """
    )

    /**
     * Should be a utility class since all fields are static, but class has public constructor
     */
    @Test
    fun identifyUtilityClassesOnlyStaticFields(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public A() {
                }

                public static int a;
            }
        """,
        after = """
            public class A {
                private A() {
                }

                public static int a;
            }
        """
    )

    /**
     * Not a utility class since the class implements an interface
     */
    @Test
    fun identifyNonUtilityClassesWhenImplementsInterface(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(
            """
            public interface B {
                static void utility() {
                }
            }
        """
        ),
        before = """
            public class A implements B {
                public A() {
                }

                public static void utility() {
                    B.utility();
                }
            }
        """
    )

    /**
     * Not a utility class since the class extends another
     */
    @Test
    fun identifyNonUtilityClassesWhenExtendsClass(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(
            """
            public class B {
                public static void utility() {}
            }
        """
        ),
        before = """
            public class A extends B {
                public A() {
                }

                public static void doSomething() {
                }
            }
        """
    )

    /**
     * Not a utility class since some fields are static, but at least one non-static
     */
    @Test
    fun identifyNonUtilityClassesMixedFields(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                public A() {
                }

                public int a;

                public static int b;
            }
        """
    )

    /**
     * Should be a utility class since all methods are static, but class has public constructor
     */
    @Test
    fun identifyUtilityClassesOnlyStaticMethods(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public A() {
                }

                public static void utility() {
                }

                public static void utility(String[] args) {
                    utility();
                }
            }
        """,
        after = """
            public class A {
                private A() {
                }

                public static void utility() {
                }

                public static void utility(String[] args) {
                    utility();
                }
            }
        """
    )

    /**
     * Inner class should be a utility class since all it's methods are static, but it has public constructor
     */
    @Test
    fun identifyUtilityClassesInnerStaticClasses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {

                static class Inner {
                    public Inner() {
                    }

                    public static void utility() {
                    }
                }

                public void utility() {
                }
            }
        """,
        after = """
            public class A {

                static class Inner {
                    private Inner() {
                    }

                    public static void utility() {
                    }
                }

                public void utility() {
                }
            }
        """
    )

    /**
     * Not a utility class since some methods are static, but at least one non-static
     */
    @Test
    fun identifyNonUtilityClassesMixedMethods(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                public A() {
                }

                public static void someStatic() {
                }

                public void notStatic() {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1060")
    @Test
    fun identifyAbstractClass(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public abstract class A {
                public A() {
                }

                public static void someStatic1() {
                }

                public static void someStatic2() {
                }
            }
        """
    )

    @Test
    fun identifyNonUtilityClassesOnlyPublicConstructor(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                public A() {
                }
            }
        """
    )

    @Test
    fun identifyNonUtilityClassesTotallyEmpty(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
            }
        """
    )

    @Test
    fun dontChangeSuppressedUtilityClasses(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(
            listOf(
                NamedStyles(
                    randomId(), "test", "test", "test", emptySet(), listOf(
                        HideUtilityClassConstructorStyle(
                            listOf(
                                "@lombok.experimental.UtilityClass",
                                "@lombok.Data",
                                """@java.lang.SuppressWarnings("checkstyle:HideUtilityClassConstructor")"""
                            )
                        )
                    )
                )
            )
        ).build(),
        dependsOn = arrayOf(
            """
            package lombok.experimental;
            public @interface UtilityClass {}
        """
        ),
        before = """
            import lombok.experimental.UtilityClass;
            
            @UtilityClass
            public class DoNotChangeMeA {
                public static void utility() {
                }
            }

            @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
            class DoNotChangeMeB {
                public static void utility() {
                }
            }
        """
    )

    @Test
    fun suppressedUtilityClassWithDifferentArgument(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(
                NamedStyles(
                    randomId(), "test", "test", "test", emptySet(), listOf(
                        HideUtilityClassConstructorStyle(
                            listOf("""@java.lang.SuppressWarnings("checkstyle:HideUtilityClassConstructor")""")
                        )
                    )
                )
            )
        ).build(),
        before = """
            @SuppressWarnings("rewrite:I-can-change")
            class ChangeMeA {
                public static void utility() {
                }
            }
        """,
        after = """
            @SuppressWarnings("rewrite:I-can-change")
            class ChangeMeA {
                public static void utility() {
                }

                private ChangeMeA() {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1058")
    @Test
    fun doesNotChangePackagePrivateEnumConstructorToPrivate(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public enum SomeEnum {
                A,B,C;

                SomeEnum() {
                }

                public static void utility() {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1058")
    @Test
    fun enumClass(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public enum SomeEnum {
                A,B,C;

                public static void utility() {
                }
            }
        """
    )
}
