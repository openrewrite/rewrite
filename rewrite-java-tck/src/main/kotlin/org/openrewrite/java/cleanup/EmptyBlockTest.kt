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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("ClassInitializerMayBeStatic", "StatementWithEmptyBody", "ConstantConditions",
    "SynchronizationOnLocalVariableOrMethodParameter", "CatchMayIgnoreException", "EmptyFinallyBlock",
    "InfiniteLoopStatement", "UnnecessaryContinue", "EmptyClassInitializer", "EmptyTryBlock"
)
interface EmptyBlockTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = EmptyBlock()

    @Suppress("ClassInitializerMayBeStatic")
    @Test
    fun emptySwitch(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    int i = 0;
                    switch(i) {
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                }
            }
        """
    )

    @Test
    fun emptyBlockWithComment(jp: JavaParser) = assertUnchanged(
        before = """
            public class A {
                {
                    // comment
                }
            }
        """
    )

    @Suppress("EmptySynchronizedStatement")
    @Test
    fun emptySynchronized(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    final Object o = new Object();
                    synchronized(o) {
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    final Object o = new Object();
                }
            }
        """
    )

    @Test
    fun emptyTry(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.io.*;

            public class A {
                {
                    final String fileName = "fileName";
                    try(FileInputStream fis = new FileInputStream(fileName)) {
                    } catch (IOException e) {
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    final String fileName = "fileName";
                }
            }
        """
    )

    @Test
    fun emptyCatchBlockWithIOException(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.nio.file.*;

            public class A {
                public void foo() {
                    try {
                        new FileInputStream(new File("somewhere"));
                    } catch (IOException e) {
                    }
                }
            }
        """
    )

    @Test
    fun emptyCatchBlockWithExceptionAndEmptyFinally(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.nio.file.*;

            public class A {
                public void foo() {
                    try {
                        new FileInputStream(new File("somewhere"));
                    } catch (Throwable t) {
                    } finally {
                    }
                }
            }
        """,
        after = """
            import java.nio.file.*;

            public class A {
                public void foo() {
                    try {
                        new FileInputStream(new File("somewhere"));
                    } catch (Throwable t) {
                    }
                }
            }
        """
    )

    @Test
    fun emptyLoops(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void foo() {
                    while(true) {
                    }
                    do {
                    } while(true);
                }
            }
        """,
        after = """
            public class A {
                public void foo() {
                    while(true) {
                        continue;
                    }
                    do {
                        continue;
                    } while(true);
                }
            }
        """
    )

    @Test
    fun emptyInstanceAndStaticInit(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                static {}
                {}
            }
        """,
        after = """
            public class A {
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun extractSideEffectsFromEmptyIfsWithNoElse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                int n = sideEffect();

                int sideEffect() {
                    return new java.util.Random().nextInt();
                }

                boolean boolSideEffect() {
                    return sideEffect() == 0;
                }

                public void lotsOfIfs() {
                    if(sideEffect() == 1) {}
                    if(sideEffect() == sideEffect()) {}
                    int n;
                    if((n = sideEffect()) == 1) {}
                    if((n /= sideEffect()) == 1) {}
                    if(new A().n == 1) {}
                    if(!boolSideEffect()) {}
                    if(1 == 2) {}
                }
            }
        """,
        after = """
            public class A {
                int n = sideEffect();

                int sideEffect() {
                    return new java.util.Random().nextInt();
                }

                boolean boolSideEffect() {
                    return sideEffect() == 0;
                }

                public void lotsOfIfs() {
                    sideEffect();
                    sideEffect();
                    sideEffect();
                    int n;
                    n = sideEffect();
                    n /= sideEffect();
                    new A();
                    boolSideEffect();
                }
            }
        """
    )

    @Test
    fun invertIfWithOnlyElseClauseAndBinaryOperator(jp: JavaParser) = assertChanged(
        jp,
        // extra spaces after the original if condition to ensure that we preserve the if statement's block formatting
        before = """
            public class A {
                {
                    if("foo".length() > 3)   {
                    } else {
                        System.out.println("this");
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    if("foo".length() <= 3)   {
                        System.out.println("this");
                    }
                }
            }
        """
    )

    @Test
    fun invertIfWithElseIfElseClause(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    if("foo".length() > 3) {
                    } else if("foo".length() > 4) {
                        System.out.println("longer");
                    }
                    else {
                        System.out.println("this");
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    if("foo".length() <= 3) {
                        if("foo".length() > 4) {
                            System.out.println("longer");
                        }
                        else {
                            System.out.println("this");
                        }
                    }
                }
            }
        """
    )

    @Test
    fun emptyElseBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    if (true) {
                        System.out.println("this");
                    } else {
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    if (true) {
                        System.out.println("this");
                    }
                }
            }
        """
    )
}
