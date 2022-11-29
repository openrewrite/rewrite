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
import org.openrewrite.Tree
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.WrappingAndBracesStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

@Suppress("UnusedAssignment", "ClassInitializerMayBeStatic")
interface WrappingAndBracesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(toRecipe {WrappingAndBracesVisitor(WrappingAndBracesStyle(WrappingAndBracesStyle.IfStatement(false)))})
    }

    fun namedStyles(styles: Collection<Style>) : Iterable<NamedStyles> {
        return listOf(NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles))
    }

    @Suppress("StatementWithEmptyBody", "ConstantConditions")
    @Issue("https://github.com/openrewrite/rewrite/issues/804")
    @Test
    fun conditionalsShouldStartOnNewLines() = rewriteRun(
        { spec -> spec.recipe(WrappingAndBraces().doNext(TabsAndIndents()))},
        java("""
            class Test {
                void test() {
                    if (1 == 2) {
                    } if (1 == 3) {
                    }
                }
            }
            """,
            """
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
    )

    @Test
    fun blockLevelStatements() = rewriteRun(
        java("""
            public class Test {
                {        int n = 0;
                    n++;
                }
            }
            """,
        """
            public class Test {
                {
                    int n = 0;
                    n++;
                }
            }
            """
        )
    )

    @Test
    fun blockEndOnOwnLine() = rewriteRun(
        java("""
            class Test {
                int n = 0;}
            """,
        """
            class Test {
                int n = 0;
            }
            """
        )
    )

    @Test
    fun annotatedMethod() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) Object method() {
                    return new Object();
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             Object method() {
                    return new Object();
                }
            }
            """
        )
    )

    @Test
    fun annotatedMethodWithModifier() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) public Object method() {
                    return new Object();
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             public Object method() {
                    return new Object();
                }
            }
            """
        )
    )

    @Test
    fun annotatedMethodWithModifiers() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) public final Object method() {
                    return new Object();
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             public final Object method() {
                    return new Object();
                }
            }
            """
        )
    )

    @Test
    fun annotatedMethodWithTypeParameter() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) <T> T method() {
                    return null;
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             <T> T method() {
                    return null;
                }
            }
            """
        )
    )

    @Test
    fun multipleAnnotatedMethod() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) @Deprecated Object method() {
                    return new Object();
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             @Deprecated
             Object method() {
                    return new Object();
                }
            }
            """
        )
    )

    @Test
    fun annotatedConstructor() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings({"ALL"}) @Deprecated Test() {
                }
            }
            """,
        """
            public class Test {
                @SuppressWarnings({"ALL"})
             @Deprecated
             Test() {
                }
            }
            """
        )
    )

    @Test
    fun annotatedClassDecl() = rewriteRun(
        java("""
            @SuppressWarnings({"ALL"}) class Test {
            }
            """,
        """
            @SuppressWarnings({"ALL"})
             class Test {
            }
            """
        )
    )

    @Test
    fun annotatedClassDeclAlreadyCorrect() = rewriteRun(
        java("""
            @SuppressWarnings({"ALL"}) 
            class Test {
            }
            """
        )
    )

    @Test
    fun annotatedClassDeclWithModifiers() = rewriteRun(
        java("""
            @SuppressWarnings({"ALL"}) public class Test {
            }
            """,
        """
            @SuppressWarnings({"ALL"})
             public class Test {
            }
            """
        )
    )

    @Test
    fun annotatedVariableDecl() = rewriteRun(
        java("""
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL") int foo;        
                }
            }
            """,
        """
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL")
             int foo;        
                }
            }
            """
        )
    )

    @Test
    fun annotatedVariableAlreadyCorrect() = rewriteRun(
        java("""
            public class Test {
                public void doSomething() {
                    @SuppressWarnings("ALL")
                    int foo;        
                }
            }
            """
        )
    )

    @Test
    fun annotatedVariableDeclWithModifier() = rewriteRun(
        java("""
            public class Test {
                @SuppressWarnings("ALL") private int foo;        
            }
            """,
        """
            public class Test {
                @SuppressWarnings("ALL")
             private int foo;        
            }
            """
        )
    )

    @Test
    fun annotatedVariableDeclInMethodDeclaration() = rewriteRun(
        java("""
            public class Test {
                public void doSomething(@SuppressWarnings("ALL") int foo) {
                }
            }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/375")
    @Test
    fun retainTrailingComments() = rewriteRun(
        java("""
            public class Test {
            int m; /* comment */ int n;}
            """,
        """
            public class Test {
            int m; /* comment */
            int n;
            }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Test
    fun elseOnNewLine() = rewriteRun(
        { spec ->
            spec.parser(
                JavaParser.fromJavaVersion().styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(true)
                        })
                    }))
                )
            ).recipe(toRecipe {WrappingAndBracesVisitor(WrappingAndBracesStyle(WrappingAndBracesStyle.IfStatement(true)))}
                .doNext(TabsAndIndents()))
        },
        java("""
            public class Test {
                void method(int arg0) {
                    if (arg0 == 0) {
                        System.out.println("if");
                    } else if (arg0 == 1) {
                        System.out.println("else if");
                    } else {
                        System.out.println("else");
                    }
                }
            }
            """,
            """
            public class Test {
                void method(int arg0) {
                    if (arg0 == 0) {
                        System.out.println("if");
                    }
                    else if (arg0 == 1) {
                        System.out.println("else if");
                    }
                    else {
                        System.out.println("else");
                    }
                }
            }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Test
    fun elseNotOnNewLine() = rewriteRun(
        { spec ->
            spec.parser(
                JavaParser.fromJavaVersion().styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(true)
                        })
                    }))
                )
            ).recipe(toRecipe {WrappingAndBracesVisitor(WrappingAndBracesStyle(WrappingAndBracesStyle.IfStatement(false)))}
                .doNext(Spaces())
                .doNext(TabsAndIndents()))
        },
        java("""
            public class Test {
                void method(int arg0) {
                    if (arg0 == 0) {
                        System.out.println("if");
                    }
                    else if (arg0 == 1) {
                        System.out.println("else if");
                    }
                    else {
                        System.out.println("else");
                    }
                }
            }
            """,
            """
            public class Test {
                void method(int arg0) {
                    if (arg0 == 0) {
                        System.out.println("if");
                    } else if (arg0 == 1) {
                        System.out.println("else if");
                    } else {
                        System.out.println("else");
                    }
                }
            }
            """
        )
    )
}
