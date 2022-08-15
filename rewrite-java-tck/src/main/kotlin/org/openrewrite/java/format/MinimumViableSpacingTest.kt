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
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.Space

@Suppress("StatementWithEmptyBody")
interface MinimumViableSpacingTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitSpace(space: Space, loc: Space.Location, ctx: ExecutionContext): Space {
                    if (ctx.getMessage<Int>("cyclesThatResultedInChanges") == 0) {
                        return space.withWhitespace("")
                    }
                    return space
                }
            }
        }.doNext(toRecipe {MinimumViableSpacingVisitor<ExecutionContext>(null)})

    @Test
    fun method(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public <T> void foo() {
                }
            }
        """,
        after = """
            class A{public <T> void foo(){}}
        """
    )

    @Test
    fun returnExpression(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public String foo() {
                    return"foo";
                }
            }
        """,
        after = """
            class A{public String foo(){return "foo";}}
        """
    )

    @Test
    fun variableDeclarationsInClass(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                int unassigned;
                int zero = 0;
                final int one = 1;
                public static final int ONE = 1;
                public static final int TWO = 1, THREE = 3;
            }
        """,
        after = """
            class A{int unassigned;int zero=0;final int one=1;public static final int ONE=1;public static final int TWO=1,THREE=3;}
        """
    )

    @Test
    fun variableDeclarationsInMethod(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void foo(int paramA, final int paramB) {
                    int unassigned;
                    int a = 1;
                    int b, c = 5;
                    final int d = 10;
                    final int e, f = 20;
                }
            }
        """,
        after = """
            class A{public void foo(int paramA,final int paramB){int unassigned;int a=1;int b,c=5;final int d=10;final int e,f=20;}}
        """
    )

    @Test
    fun variableDeclarationsInForLoops(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void foo(final int[] arr) {
                    for (int n = 0, x = 0; n < 100; n++, x++) {
                    }
                    
                    for (int i: arr) {
                    }
                    
                    for (final int i: arr) {
                    }
                }
            }
        """,
        after = """
            class Test{void foo(final int[] arr){for(int n=0,x=0;n<100;n++,x++){}for(int i:arr){}for(final int i:arr){}}}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/758")
    @Test
    fun spacesBetweenModifiers(jp: JavaParser) = assertChanged(
        before = """
            public final class A {
                public static String foo() { 
                    return "foo"; 
                } 
            }
        """,
        after = """
            public final class A{public static String foo(){return "foo";}}
        """
    )
}
