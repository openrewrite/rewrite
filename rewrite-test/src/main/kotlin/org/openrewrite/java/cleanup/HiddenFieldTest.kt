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
@file:Suppress("UnnecessaryLocalVariable")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.Checkstyle
import org.openrewrite.java.style.HiddenFieldStyle
import org.openrewrite.style.NamedStyles

interface HiddenFieldTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = HiddenField()

    fun hiddenFieldStyle(with: HiddenFieldStyle.() -> HiddenFieldStyle = { this }) =
        listOf(
            NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                    Checkstyle.hiddenFieldStyle().run { with(this) }
                )
            )
        )

    @Test
    fun ignoreUnaffectedVariables(jp: JavaParser) = assertUnchanged(
        // basic check to ensure this recipe doesn't rename every variable no matter what
        jp,
        before = """
            package org.openrewrite;

            public class A {
                private String field;

                public A(String someField) {
                }

                public void method(String someField) {
                    String localVariable = someField;
                }
            }
        """
    )

    @Test
    fun renameHiddenFields(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
            package org.openrewrite;

            public class B {
                protected int n2;
                int n3;
                private int n4;
            }
            """
        ),
        before = """
            package org.openrewrite;

            public class A extends B {
                int n;
                int n1;

                class C {
                    public void method(int n) {
                        int n1 = n;
                    }
                }

                static class D {
                    public void method(int n) {
                    }
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A extends B {
                int n;
                int n1;

                class C {
                    public void method(int n2) {
                        int n3 = n2;
                    }
                }

                static class D {
                    public void method(int n) {
                    }
                }
            }
        """
    )

    @Test
    fun constructorParameter(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreConstructorParameter(false)
        }).build(),
        before = """
            package org.openrewrite;

            public class A {
                private String field;

                public A(String field) {
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                private String field;

                public A(String field1) {
                }
            }
        """
    )

    @Test
    fun ignoreConstructorParameter(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreConstructorParameter(true)
        }).build(),
        before = """
            package org.openrewrite;

            public class A {
                private String field;

                public A(String field) {
                }
            }
        """
    )

    @Test
    fun methodParameter(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class A {
                private String field;

                public void method(String field) {
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                private String field;

                public void method(String field1) {
                }
            }
        """
    )

    @Test
    fun methodBodyLocalVariable(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class A {
                private String field;

                public void method(String param) {
                    String field = param;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                private String field;

                public void method(String param) {
                    String field1 = param;
                }
            }
        """
    )

    @Test
    fun forLoops(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class A {
                int n;

                public void standardForLoop() {
                    for (int n = 0; n < 1; n++) {
                        int x = n;
                    }
                }

                public void enhancedForLoop(int[] arr) {
                    for (int n : arr) {
                        int x = n;
                    }
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                int n;

                public void standardForLoop() {
                    for (int n1 = 0; n1 < 1; n1++) {
                        int x = n1;
                    }
                }

                public void enhancedForLoop(int[] arr) {
                    for (int n1 : arr) {
                        int x = n1;
                    }
                }
            }
        """
    )

    @Suppress("Convert2MethodRef", "ResultOfMethodCallIgnored")
    @Test
    fun lambdaWithTypedParameterHides(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;
            
            import java.util.List;
            import java.util.Arrays;

            public class A {
                List<Integer> numbers = Arrays.asList(1, 2, 3);
                Integer value = 0;
                {
                    numbers.forEach((Integer value) -> String.valueOf(value));
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.util.List;
            import java.util.Arrays;

            public class A {
                List<Integer> numbers = Arrays.asList(1, 2, 3);
                Integer value = 0;
                {
                    numbers.forEach((Integer value1) -> String.valueOf(value1));
                }
            }
        """
    )

    @Test
    fun nestedClasses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class Outer {
                int outer;

                public class Inner {
                    int inner;

                    public Inner() {
                        int inner = 0;
                    }

                    public Inner(int inner) {
                    }

                    public void method() {
                        int outer = 0;
                    }
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class Outer {
                int outer;

                public class Inner {
                    int inner;

                    public Inner() {
                        int inner1 = 0;
                    }

                    public Inner(int inner) {
                    }

                    public void method() {
                        int outer1 = 0;
                    }
                }
            }
        """
    )

    @Test
    fun incrementRenamedVariableNameUntilUnique(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class A {
                int n, n1;

                public void method(int n) {
                    int n1 = 0;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                int n, n1;

                public void method(int n2) {
                    int n3 = 0;
                }
            }
        """
    )

    @Test
    fun incrementRenamedVariableNameShouldNotCollideWithExistingVariablesInUse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public class A {
                int n, n1;

                public void method(int n) {
                    int n2 = 0;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                int n, n1;

                public void method(int n3) {
                    int n2 = 0;
                }
            }
        """
    )

    @Test
    fun ignoreEnums(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package org.openrewrite;

            enum ExampleEnum {
                A(0),
                B(1),
                C(2) {
                    int hidden;

                    public void method() {
                        int hidden = 0;
                    }
                };

                int hidden;
                static int hiddenStatic;

                ExampleEnum(int hidden) {
                }

                public void method() {
                    int hidden = 0;
                }

                public static void methodStatic() {
                    int hiddenStatic = 0;
                }
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun ignoreStaticMethodsAndInitializers(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package org.openrewrite;

            public class StaticMethods {
                private int notHidden;

                public static void method() {
                    // local variables of static methods don't hide instance fields.
                    int notHidden;
                }

                static {
                    // local variables of static initializers don't hide instance fields.
                    int notHidden;
                }

                private int x;
                private static int y;

                static class Inner {
                    void useX(int x) {
                        x++;
                    }

                    void useY(int y) {
                        y++;
                    }
                }
            }
        """
    )

    @Test
    fun ignoreInterfaces(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package org.openrewrite;

            interface A {
                int n = 0;

                void method(int n);
            }
        """
    )

    @Test
    fun renamesSetters(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreSetter(false)
        }).build(),
        before = """
            package org.openrewrite;

            class A {
                int n;

                public void setN(int n) {
                    this.n = n;
                }
            }
    
            class B {
                int n;

                public B setN(int n) {
                    this.n = n;
                    return this;
                }
            }
        """,
        after = """
            package org.openrewrite;

            class A {
                int n;

                public void setN(int n1) {
                    this.n = n1;
                }
            }
    
            class B {
                int n;

                public B setN(int n1) {
                    this.n = n1;
                    return this;
                }
            }
        """
    )

    @Test
    fun ignoreSetter(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreSetter(true)
                .withSetterCanReturnItsClass(false)
        }).build(),
        before = """
            package org.openrewrite;

            class A {
                int n;

                public void setN(int n) {
                    this.n = n;
                }
            }
    
            class B {
                int n;

                public B setN(int n) {
                    this.n = n;
                    return this;
                }
            }
        """,
        after = """
            package org.openrewrite;

            class A {
                int n;

                public void setN(int n) {
                    this.n = n;
                }
            }

            class B {
                int n;

                public B setN(int n1) {
                    this.n = n1;
                    return this;
                }
            }
        """
    )

    @Test
    fun ignoreSetterThatReturnsItsClass(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreSetter(true)
                .withSetterCanReturnItsClass(true)
        }).build(),
        before = """
            package org.openrewrite;

            class B {
                int n;

                public B setN(int n) {
                    this.n = n;
                    return this;
                }
            }
        """
    )

    @Test
    fun renamesAbstractMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;

            public abstract class A {
                int n;

                public abstract void method(int n);
            }
        """,
        after = """
            package org.openrewrite;

            public abstract class A {
                int n;

                public abstract void method(int n1);
            }
        """
    )

    @Test
    fun ignoreAbstractMethodParameters(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(hiddenFieldStyle {
            withIgnoreAbstractMethods(true)
        }).build(),
        before = """
            package org.openrewrite;

            public abstract class A {
                int n;

                public abstract void method(int n);
            }
        """
    )

    @Test
    fun renamingVariableInSubclassShouldNotTakeSuperclassFieldsIntoAccount(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
            package org.openrewrite;

            public class B {
                protected Integer n2;
                Integer n3;
                private Integer n4;
            }
        """
        ),
        before = """
            package org.openrewrite;

            public class A extends B {
                Integer n;
                Integer n1;

                class C {
                    public void method(Integer n) {
                        Integer n1 = n;
                    }
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A extends B {
                Integer n;
                Integer n1;

                class C {
                    public void method(Integer n2) {
                        Integer n3 = n2;
                    }
                }
            }
        """
    )

}
