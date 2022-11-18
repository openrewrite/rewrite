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
package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.WrappingAndBracesStyle

@Suppress("UnusedAssignment", "ClassInitializerMayBeStatic")
interface WrappingAndBracesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = toRecipe {WrappingAndBracesVisitor(WrappingAndBracesStyle(WrappingAndBracesStyle.IfStatement(false)))}

    @Suppress("StatementWithEmptyBody", "ConstantConditions")
    @Issue("https://github.com/openrewrite/rewrite/issues/804")
    @Test
    fun conditionalsShouldStartOnNewLines(jp: JavaParser) = assertChanged(
        jp,
        recipe = WrappingAndBraces().doNext(TabsAndIndents()),
        before = """
            class Test {
                void test() {
                    if (1 == 2) {
                    } if (1 == 3) {
                    }
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    if (1 == 2) {
                    }
                    if (1 == 3) {
                    }
                }
            }
        """
    )

    @Test
    fun blockLevelStatements(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                {        int n = 0;
                    n++;
                }
            }
        """,
        after = """
            public class Test {
                {
                    int n = 0;
                    n++;
                }
            }
        """
    )

    @Test
    fun blockEndOnOwnLine(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int n = 0;}
        """,
        after = """
            class Test {
                int n = 0;
            }
        """
    )

    @Test
    fun annotatedMethod(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) Object method() {
                    return new Object();
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             Object method() {
                    return new Object();
                }
            }
        """
    )

    @Test
    fun annotatedMethodWithModifier(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) public Object method() {
                    return new Object();
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             public Object method() {
                    return new Object();
                }
            }
        """
    )

    @Test
    fun annotatedMethodWithModifiers(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) public final Object method() {
                    return new Object();
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             public final Object method() {
                    return new Object();
                }
            }
        """
    )

    @Test
    fun annotatedMethodWithTypeParameter(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) <T> T method() {
                    return null;
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             <T> T method() {
                    return null;
                }
            }
        """
    )

    @Test
    fun multipleAnnotatedMethod(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) @Deprecated Object method() {
                    return new Object();
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             @Deprecated
             Object method() {
                    return new Object();
                }
            }
        """
    )

    @Test
    fun annotatedConstructor(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings({"ALL"}) @Deprecated Test() {
                }
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings({"ALL"})
             @Deprecated
             Test() {
                }
            }
        """
    )

    @Test
    fun annotatedClassDecl(jp: JavaParser) = assertChanged(
        jp,
        before = """
            @SuppressWarnings({"ALL"}) class Test {
            }
        """,
        after = """
            @SuppressWarnings({"ALL"})
             class Test {
            }
        """
    )

    @Test
    fun annotatedClassDeclAlreadyCorrect(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            @SuppressWarnings({"ALL"}) 
            class Test {
            }
        """
    )

    @Test
    fun annotatedClassDeclWithModifiers(jp: JavaParser) = assertChanged(
        jp,
        before = """
            @SuppressWarnings({"ALL"}) public class Test {
            }
        """,
        after = """
            @SuppressWarnings({"ALL"})
             public class Test {
            }
        """
    )

    @Test
    fun annotatedVariableDecl(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL") int foo;        
                }
            }
        """,
        after = """
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL")
             int foo;        
                }
            }
        """
    )

    @Test
    fun annotatedVariableAlreadyCorrect(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL")
                    int foo;        
                }
            }
        """,
    )

    @Test
    fun annotatedVariableDeclWithModifier(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                @SuppressWarnings("ALL") private int foo;        
            }
        """,
        after = """
            public class Test {
                @SuppressWarnings("ALL")
             private int foo;        
            }
        """
    )

    @Test
    fun annotatedVariableDeclInMethodDeclaration(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class Test {
                public void doSomething(@SuppressWarnings("ALL") int foo) {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/375")
    @Test
    fun retainTrailingComments(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
            int m; /* comment */ int n;}
        """,
        after = """
            public class Test {
            int m; /* comment */
            int n;
            }
        """
    )
}
