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
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.NeedBracesStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

@Suppress(
    "InfiniteLoopStatement",
    "IfStatementWithIdenticalBranches",
    "LoopStatementThatDoesntLoop",
    "StatementWithEmptyBody",
    "UnusedAssignment",
    "ConstantConditions",
    "ClassInitializerMayBeStatic",
    "UnnecessaryReturnStatement"
)
interface NeedBracesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NeedBraces()

    fun namedStyles(styles: Collection<Style>): Iterable<NamedStyles> {
        return listOf(NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles))
    }

    @Test
    fun addBraces(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            NeedBracesStyle(
                false,
                false
            )
        ))).build(),
        before = """
            class Test {
                static void addToWhile() {
                    while (true) ;
                }

                static void addToWhileWithBody() {
                    while (true) return;
                }

                static void addToIf(int n) {
                    if (n == 1) return;
                }

                static void addToIfElse(int n) {
                    if (n == 1) return;
                    else return;
                }

                static void addToIfElseIfElse(int n) {
                    if (n == 1) return;
                    else if (n == 2) return;
                    else return;
                }

                static void addToDoWhile(Object obj) {
                    do obj.notify(); while (true);
                }

                static void addToIterativeFor(Object obj) {
                    for (int i = 0; ; ) obj.notify();
                }
            }
        """,
        after = """
            class Test {
                static void addToWhile() {
                    while (true) {
                    }
                }

                static void addToWhileWithBody() {
                    while (true) {
                        return;
                    }
                }

                static void addToIf(int n) {
                    if (n == 1) {
                        return;
                    }
                }

                static void addToIfElse(int n) {
                    if (n == 1) {
                        return;
                    } else {
                        return;
                    }
                }

                static void addToIfElseIfElse(int n) {
                    if (n == 1) {
                        return;
                    } else if (n == 2) {
                        return;
                    } else {
                        return;
                    }
                }

                static void addToDoWhile(Object obj) {
                    do {
                        obj.notify();
                    } while (true);
                }

                static void addToIterativeFor(Object obj) {
                    for (int i = 0; ; ) {
                        obj.notify();
                    }
                }
            }
        """
    )

    @Test
    fun allowEmptyLoopBody(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(namedStyles(listOf(
            NeedBracesStyle(
                false,
                true
            )
        ))).build(),
        before = """
            class Test {
                static void emptyWhile() {
                    while (true) ;
                }

                static void emptyForIterative() {
                    for (int i = 0; i < 10; i++) ;
                }
            }
        """
    )

    @Test
    fun allowSingleLineStatement(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(namedStyles(listOf(
            NeedBracesStyle(
                true,
                false
            )
        ))).build(),
        before = """
            class Test {
                static void allowIf(int n) {
                    if (n == 1) return;
                }

                static void allowIfElse(int n) {
                    if (n == 1) return;
                    else return;
                }

                static void allowIfElseIfElse(int n) {
                    if (n == 1) return;
                    else if (n == 2) return;
                    else return;
                }

                static void allowWhileWithBody() {
                    while (true) return;
                }

                static void allowDoWhileWithBody(Object obj) {
                    do obj.notify(); while (true);
                }

                static void allowForIterativeWithBody(Object obj) {
                    for (int i = 0; ; ) obj.notify();
                }
            }
        """
    )

    @Test
    fun doNotAllowLoopsWithEmptyBodyWhenSingleLineStatementAreAllowed(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            NeedBracesStyle(
                true,
                false
            )
        ))).build(),
        before = """
            class Test {
                static void doNotAllowWhileWithEmptyBody() {
                    while (true) ;
                }

                static void doNotAllowDoWhileWithEmptyBody(Object obj) {
                    do ; while (true);
                }

                static void doNotAllowForIterativeWithEmptyBody(Object obj) {
                    for (int i = 0; ; ) ;
                }
            }
        """,
        after = """
            class Test {
                static void doNotAllowWhileWithEmptyBody() {
                    while (true) {
                    }
                }

                static void doNotAllowDoWhileWithEmptyBody(Object obj) {
                    do {
                    } while (true);
                }

                static void doNotAllowForIterativeWithEmptyBody(Object obj) {
                    for (int i = 0; ; ) {
                    }
                }
            }
        """
    )

    @Test
    fun allowSingleLineStatementInSwitch(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(namedStyles(listOf(
            NeedBracesStyle(
                true,
                false
            )
        ))).build(),
        before = """
            class Test {
                {
                    int counter = 0;
                    int n = 1;
                    switch (n) {
                      case 1: counter++; break;
                      case 6: counter += 10; break;
                      default: counter = 100; break;
                    }
                }
            }
        """
    )

    @Test
    fun initializeStyleWhenOtherwiseNotProvided(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    if (true) {
                        return;
                    }
                }
            }
        """
    )

}
