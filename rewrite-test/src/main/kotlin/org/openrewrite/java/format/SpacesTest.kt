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
@file:Suppress("InfiniteRecursion")

package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.EmptyForInitializerPadStyle
import org.openrewrite.java.style.EmptyForIteratorPadStyle
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

interface SpacesTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = Spaces()

    fun namedStyles(styles: Collection<Style>) : Iterable<NamedStyles> {
        return listOf(NamedStyles(randomId(), "Test", "test", "test", emptySet(), styles))
    }

    @Test
    fun beforeParensMethodDeclarationTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodDeclaration(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void method1() {
                    }
                }
            """,
            after = """
                class Test {
                    void method1 () {
                    }
                }
            """
    )

    @Test
    fun beforeClassBody(jp: JavaParser) = assertChanged(
            jp,
            before = """
            class Test{
            }
        """,
            after = """
            class Test {
            }
        """
    )

    @Test
    fun beforeParensMethodDeclarationFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodDeclaration(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void method1 () {
                    }
                }
            """,
            after = """
                class Test {
                    void method1() {
                    }
                }
            """
    )

    @Test
    fun beforeParensMethodCallTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodCall(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        foo();
                        Test test = new Test();
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        foo ();
                        Test test = new Test ();
                    }
                }
            """
    )

    @Test
    fun beforeParensMethodCallFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodCall(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        foo ();
                        Test test = new Test ();
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        foo();
                        Test test = new Test();
                    }
                }
            """
    )

    @Test
    fun beforeParensIfParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withIfParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        if (true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        if(true) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensIfParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withIfParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        if(true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        if (true) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensForParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withForParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (; ; ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for(; ; ) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensForParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withForParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for(; ; ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (; ; ) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensWhileParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withWhileParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        while (true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        while(true) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensWhileParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withWhileParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        while(true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        while (true) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensSwitchParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSwitchParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        switch (0) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        switch(0) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensSwitchParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSwitchParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        switch(0) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        switch (0) {
                        }
                    }
                }
            """
    )

    val tryResource: Array<String>
        get() = arrayOf("""
                    class MyResource implements AutoCloseable {
                        public void close() {
                        }
                    }
                """)

    @Test
    fun beforeParensTryParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withTryParentheses(false)
                        })
                    }))
            ).build(),
            dependsOn = tryResource,
            before = """
                class Test {
                    void foo() {
                        try (MyResource res = new MyResource()) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        try(MyResource res = new MyResource()) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensTryParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withTryParentheses(true)
                        })
                    }))
            ).build(),
            dependsOn = tryResource,
            before = """
                class Test {
                    void foo() {
                        try(MyResource res = new MyResource()) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        try (MyResource res = new MyResource()) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensCatchParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withCatchParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        try {
                        } catch(Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensCatchParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withCatchParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        try {
                        } catch(Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensSynchronizedParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSynchronizedParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        synchronized (new Object()) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        synchronized(new Object()) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensSynchronizedParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSynchronizedParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        synchronized(new Object()) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        synchronized (new Object()) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensAnnotationParametersTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withAnnotationParameters(true)
                        })
                    }))
            ).build(),
            before = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """,
            after = """
                @SuppressWarnings ({"ALL"})
                class Test {
                }
            """
    )

    @Test
    fun beforeParensAnnotationParametersFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withAnnotationParameters(false)
                        })
                    }))
            ).build(),
            before = """
                @SuppressWarnings ({"ALL"})
                class Test {
                }
            """,
            after = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """
    )

    @Test
    fun aroundOperatorsAssignmentFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAssignment(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x = 0;
                        x += 1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x=0;
                        x+=1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsAssignmentTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAssignment(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x=0;
                        x+=1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 0;
                        x += 1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsLogicalFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLogical(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean x = true && false;
                        boolean y = true || false;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean x = true&&false;
                        boolean y = true||false;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsLogicalTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLogical(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean x = true&&false;
                        boolean y = true||false;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean x = true && false;
                        boolean y = true || false;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsEqualityFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withEquality(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean x = 0 == 1;
                        boolean y = 0 != 1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean x = 0==1;
                        boolean y = 0!=1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsEqualityTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withEquality(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean x = 0==1;
                        boolean y = 0!=1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean x = 0 == 1;
                        boolean y = 0 != 1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsRelationalFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withRelational(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean a = 0 < 1;
                        boolean b = 0 <= 1;
                        boolean c = 0 >= 1;
                        boolean d = 0 >= 1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean a = 0<1;
                        boolean b = 0<=1;
                        boolean c = 0>=1;
                        boolean d = 0>=1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsRelationalTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withRelational(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        boolean a = 0<1;
                        boolean b = 0<=1;
                        boolean c = 0>=1;
                        boolean d = 0>=1;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        boolean a = 0 < 1;
                        boolean b = 0 <= 1;
                        boolean c = 0 >= 1;
                        boolean d = 0 >= 1;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsBitwiseFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withBitwise(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1 & 2;
                        int b = 1 | 2;
                        int c = 1 ^ 2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1&2;
                        int b = 1|2;
                        int c = 1^2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsBitwiseTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withBitwise(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1&2;
                        int b = 1|2;
                        int c = 1^2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1 & 2;
                        int b = 1 | 2;
                        int c = 1 ^ 2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsAdditiveFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAdditive(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x = 1 + 2;
                        int y = 1 - 2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 1+2;
                        int y = 1-2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsAdditiveTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAdditive(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x = 1+2;
                        int y = 1-2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 1 + 2;
                        int y = 1 - 2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsMultiplicativeFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMultiplicative(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1 * 2;
                        int b = 1 / 2;
                        int c = 1 % 2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1*2;
                        int b = 1/2;
                        int c = 1%2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsMultiplicativeTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMultiplicative(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1*2;
                        int b = 1/2;
                        int c = 1%2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1 * 2;
                        int b = 1 / 2;
                        int c = 1 % 2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsShiftFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withShift(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1 >> 2;
                        int b = 1 << 2;
                        int c = 1 >>> 2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1>>2;
                        int b = 1<<2;
                        int c = 1>>>2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsShiftTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withShift(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int a = 1>>2;
                        int b = 1<<2;
                        int c = 1>>>2;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int a = 1 >> 2;
                        int b = 1 << 2;
                        int c = 1 >>> 2;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsUnaryTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withUnary(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x = 0;
                        x++;
                        x--;
                        --x;
                        ++x;
                        x = -x;
                        x = +x;
                        boolean y = false;
                        y = !y;
                        x = ~x;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 0;
                        x ++;
                        x --;
                        -- x;
                        ++ x;
                        x = - x;
                        x = + x;
                        boolean y = false;
                        y = ! y;
                        x = ~ x;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsUnaryFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withUnary(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int x = 0;
                        x ++;
                        x --;
                        -- x;
                        ++ x;
                        x = - x;
                        x = + x;
                        boolean y = false;
                        y = ! y;
                        x = ~ x;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 0;
                        x++;
                        x--;
                        --x;
                        ++x;
                        x = -x;
                        x = +x;
                        boolean y = false;
                        y = !y;
                        x = ~x;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsLambdaArrowFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLambdaArrow(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        Runnable r = () -> {};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        Runnable r = ()->{};
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsLambdaArrowTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLambdaArrow(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        Runnable r = ()->{};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        Runnable r = () -> {};
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsMethodReferenceDoubleColonTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMethodReferenceDoubleColon(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        Runnable r1 = this::foo;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        Runnable r1 = this :: foo;
                    }
                }
            """
    )

    @Test
    fun aroundOperatorsMethodReferenceDoubleColonFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMethodReferenceDoubleColon(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        Runnable r1 = this :: foo;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        Runnable r1 = this::foo;
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceClassLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withClassLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                }
            """,
            after = """
                class Test{
                }
            """
    )

    @Test
    fun beforeLeftBraceClassLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withClassLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                }
            """,
            after = """
                class Test {
                }
            """
    )

    @Test
    fun beforeLeftBraceMethodLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withMethodLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                    public void foo() {
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(){
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceMethodLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withMethodLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                    public void foo(){
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceIfLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withIfLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                    public void foo() {
                        if (true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceIfLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withIfLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                    public void foo() {
                        if (true){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceElseLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withElseLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test{
                    public void foo() {
                        if (true) {
                        } else {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        } else{
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceElseLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withElseLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        if (true) {
                        } else{
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        } else {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceForLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withForLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                        for (int i : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++){
                        }
                        for (int i : new int[]{1, 2, 3}){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceForLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withForLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++){
                        }
                        for (int i : new int[]{1, 2, 3}){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                        for (int i : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceWhileLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withWhileLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        while (true != false) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        while (true != false){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceWhileLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withWhileLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        while (true != false){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        while (true != false) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceDoLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withDoLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        do {
                        } while (true != false);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        do{
                        } while (true != false);
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceDoLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withDoLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        do{
                        } while (true != false);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        do {
                        } while (true != false);
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceSwitchLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSwitchLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        switch (1) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        switch (1){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceSwitchLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSwitchLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        switch (1){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        switch (1) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceTryLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withTryLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try{
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceTryLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withTryLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try{
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceCatchLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withCatchLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceCatchLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withCatchLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

        @Suppress("CatchMayIgnoreException", "EmptyTryBlock")
    @Issue("https://github.com/openrewrite/rewrite/issues/1896")
    @Test
    fun aroundExceptionDelimiterFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(
            namedStyles(listOf(IntelliJ.spaces().run {
                withAroundOperators(aroundOperators.run {
                    withBitwise(false)
                })
            }))
        ).build(),
        before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (IllegalAccessException | IllegalStateException | IllegalArgumentException e) {
                        }
                    }
                }
            """,
        after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (IllegalAccessException|IllegalStateException|IllegalArgumentException e) {
                        }
                    }
                }
            """
    )

    @Suppress("CatchMayIgnoreException", "EmptyTryBlock")
    @Issue("https://github.com/openrewrite/rewrite/issues/1896")
    @Test
    fun aroundExceptionDelimiterTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(
            namedStyles(listOf(IntelliJ.spaces().run {
                withAroundOperators(aroundOperators.run {
                    withBitwise(true)
                })
            }))
        ).build(),
        before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (IllegalAccessException|IllegalStateException|IllegalArgumentException e) {
                        }
                    }
                }
            """,
        after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (IllegalAccessException | IllegalStateException | IllegalArgumentException e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceFinallyLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withFinallyLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally{
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceFinallyLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withFinallyLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally{
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceSynchronizedLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSynchronizedLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        synchronized (this) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        synchronized (this){
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceSynchronizedLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSynchronizedLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        synchronized (this){
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        synchronized (this) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceArrayInitializerLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withArrayInitializerLeftBrace(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] arr = new int[] {1, 2, 3};
                    }
                }
            """
    )

    @Test
    fun beforeLeftBraceArrayInitializerLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withArrayInitializerLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] arr = new int[] {1, 2, 3};
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1162")
    @Test
    fun beforeLeftBraceAnnotationArrayInitializerLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withAnnotationArrayInitializerLeftBrace(true)
                        })
                    }))
            ).build(),
            dependsOn = arrayOf("""
                package abc;
                @interface MyAnno {
                    String[] names;
                    Integer[] counts;
                }
            """),
            before = """
                package abc;
                @MyAnno(names={"a","b"},counts={1,2})
                class Test {
                }
            """,
            after = """
                package abc;
                @MyAnno(names = {"a", "b"}, counts = {1, 2})
                class Test {
                }
            """
    )

    @Test
    fun beforeLeftBraceAnnotationArrayInitializerLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withAnnotationArrayInitializerLeftBrace(false)
                        })
                    }))
            ).build(),
            before = """
                @SuppressWarnings( {"ALL"})
                class Test {
                }
            """,
            after = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """
    )

    @Test
    fun beforeKeywordsElseKeywordFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        if (true) {
                        } else {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        }else {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsElseKeywordTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        if (true) {
                        }else {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        } else {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsWhileKeywordFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withWhileKeyword(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        do {
                        } while (true);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        do {
                        }while (true);
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsWhileKeywordTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withWhileKeyword(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        do {
                        }while (true);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        do {
                        } while (true);
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsCatchKeywordFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withCatchKeyword(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        }catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsCatchKeywordTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withCatchKeyword(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        }catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsFinallyKeywordFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withFinallyKeyword(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }finally {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeKeywordsFinallyKeywordTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withFinallyKeyword(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }finally {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        } finally {
                        }
                    }
                }
            """
    )

    @Test
    fun withinCodeBracesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCodeBraces(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {}
                
                interface ITest {}
            """,
            after = """
                class Test { }
                
                interface ITest { }
            """
    )

    @Test
    fun withinCodeBracesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCodeBraces(false)
                        })
                    }))
            ).build(),
            before = """
                class Test { }
                
                interface ITest { }
            """,
            after = """
                class Test {}
                
                interface ITest {}
            """
    )

    @Test
    fun withinBracketsTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withBrackets(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo(int[] a) {
                        int x = a[0];
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(int[] a) {
                        int x = a[ 0 ];
                    }
                }
            """
    )

    @Test
    fun withinBracketsFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withBrackets(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo(int[] a) {
                        int x = a[ 0 ];
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(int[] a) {
                        int x = a[0];
                    }
                }
            """
    )

    @Test
    fun withinArrayInitializerBracesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withArrayInitializerBraces(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] x = {1, 2, 3};
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] x = { 1, 2, 3 };
                    }
                }
            """
    )

    @Test
    fun withinArrayInitializerBracesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withArrayInitializerBraces(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] x = { 1, 2, 3 };
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] x = {1, 2, 3};
                    }
                }
            """
    )

    @Test
    fun withinEmptyArrayInitializerBracesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyArrayInitializerBraces(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] x = {};
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] x = { };
                    }
                }
            """
    )

    @Test
    fun withinEmptyArrayInitializerBracesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyArrayInitializerBraces(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int[] x = { };
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int[] x = {};
                    }
                }
            """
    )

    @Test
    fun withinGroupingParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withGroupingParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo(int x) {
                        x += (x + 1);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(int x) {
                        x += ( x + 1 );
                    }
                }
            """
    )

    @Test
    fun withinGroupingParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withGroupingParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo(int x) {
                        x += ( x + 1 );
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(int x) {
                        x += (x + 1);
                    }
                }
            """
    )

    @Test
    fun withinMethodDeclarationParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodDeclarationParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo(int x) {
                    }
                }
            """,
            after = """
                class Test {
                    public void foo( int x ) {
                    }
                }
            """
    )

    @Test
    fun withinMethodDeclarationParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodDeclarationParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo( int x ) {
                    }
                }
            """,
            after = """
                class Test {
                    public void foo(int x) {
                    }
                }
            """
    )

    @Test
    fun withinEmptyMethodDeclarationParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodDeclarationParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                    }
                }
            """,
            after = """
                class Test {
                    public void foo( ) {
                    }
                }
            """
    )

    @Test
    fun withinEmptyMethodDeclarationParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodDeclarationParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo( ) {
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                    }
                }
            """
    )

    @Test
    fun withinMethodCallParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodCallParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void bar(int x) {
                    }
                    public void foo() {
                        bar(1);
                    }
                }
            """,
            after = """
                class Test {
                    public void bar(int x) {
                    }
                    public void foo() {
                        bar( 1 );
                    }
                }
            """
    )

    @Test
    fun withinMethodCallParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodCallParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void bar(int x) {
                    }
                    public void foo() {
                        bar( 1 );
                    }
                }
            """,
            after = """
                class Test {
                    public void bar(int x) {
                    }
                    public void foo() {
                        bar(1);
                    }
                }
            """
    )

    @Test
    fun withinEmptyMethodCallParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodCallParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void bar() {
                    }
                    public void foo() {
                        bar();
                    }
                }
            """,
            after = """
                class Test {
                    public void bar() {
                    }
                    public void foo() {
                        bar( );
                    }
                }
            """
    )

    @Test
    fun withinEmptyMethodCallParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodCallParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void bar() {
                    }
                    public void foo() {
                        bar( );
                    }
                }
            """,
            after = """
                class Test {
                    public void bar() {
                    }
                    public void foo() {
                        bar();
                    }
                }
            """
    )

    @Test
    fun withinIfParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withIfParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        if (true) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if ( true ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinIfParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withIfParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        if ( true ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        if (true) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinForParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withForParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                        for (int j : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        for ( int i = 0; i < 10; i++ ) {
                        }
                        for ( int j : new int[]{1, 2, 3} ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinForParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withForParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        for ( int i = 0; i < 10; i++ ) {
                        }
                        for ( int j : new int[]{1, 2, 3} ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                        for (int j : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinWhileParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withWhileParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        while (true) {
                        }
                        do {
                        } while (true);
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        while ( true ) {
                        }
                        do {
                        } while ( true );
                    }
                }
            """
    )

    @Test
    fun withinWhileParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withWhileParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        while ( true ) {
                        }
                        do {
                        } while ( true );
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        while (true) {
                        }
                        do {
                        } while (true);
                    }
                }
            """
    )

    @Test
    fun withinSwitchParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSwitchParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        switch (1) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        switch ( 1 ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinSwitchParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSwitchParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        switch ( 1 ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        switch (1) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinTryParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTryParentheses(true)
                        })
                    }))
            ).build(),
            dependsOn = tryResource,
            before = """
                class Test {
                    public void foo() {
                        try (MyResource res = new MyResource()) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try ( MyResource res = new MyResource() ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinTryParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTryParentheses(false)
                        })
                    }))
            ).build(),
            dependsOn = tryResource,
            before = """
                class Test {
                    public void foo() {
                        try ( MyResource res = new MyResource() ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try (MyResource res = new MyResource()) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinCatchParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCatchParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch ( Exception e ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinCatchParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCatchParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        try {
                        } catch ( Exception e ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        try {
                        } catch (Exception e) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinSynchronizedParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSynchronizedParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        synchronized (this) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        synchronized ( this ) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinSynchronizedParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSynchronizedParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        synchronized ( this ) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        synchronized (this) {
                        }
                    }
                }
            """
    )

    @Test
    fun withinTypeCastParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTypeCastParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int i = (int) 0.0d;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int i = ( int ) 0.0d;
                    }
                }
            """
    )

    @Test
    fun withinTypeCastParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTypeCastParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        int i = ( int ) 0.0d;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        int i = (int) 0.0d;
                    }
                }
            """
    )

    @Test
    fun withinAnnotationParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withAnnotationParentheses(true)
                        })
                    }))
            ).build(),
            before = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """,
            after = """
                @SuppressWarnings( {"ALL"} )
                class Test {
                }
            """
    )

    @Test
    fun withinAnnotationParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withAnnotationParentheses(false)
                        })
                    }))
            ).build(),
            before = """
                @SuppressWarnings( {"ALL"} )
                class Test {
                }
            """,
            after = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """
    )

    @Test
    fun withinAngleBracketsTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withAngleBrackets(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test<T, U> {
                
                    <T2 extends T> T2 foo() {
                        List<T2> myList = new ArrayList<>();
                        return null;
                    }
                }
            """,
            after = """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test< T, U > {
                
                    < T2 extends T > T2 foo() {
                        List< T2 > myList = new ArrayList<>();
                        return null;
                    }
                }
            """
    )

    @Test
    fun withinAngleBracketsFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withAngleBrackets(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test< T, U > {
                
                    < T2 extends T > T2 foo() {
                        List< T2 > myList = new ArrayList<>();
                        return null;
                    }
                }
            """,
            after = """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test<T, U> {
                
                    <T2 extends T> T2 foo() {
                        List<T2> myList = new ArrayList<>();
                        return null;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorBeforeQuestionMarkFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withBeforeQuestionMark(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b? 1 : 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorBeforeQuestionMarkTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withBeforeQuestionMark(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b? 1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorAfterQuestionMarkFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withAfterQuestionMark(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ?1 : 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorAfterQuestionMarkTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withAfterQuestionMark(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ?1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorBeforeColonFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withBeforeColon(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1: 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorBeforeColonTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withBeforeColon(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1: 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorAfterColonFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withAfterColon(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 :0;
                    }
                }
            """
    )

    @Test
    fun ternaryOperatorAfterColonTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTernaryOperator(ternaryOperator.run {
                            withAfterColon(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 :0;
                    }
                }
            """,
            after = """
                class Test {
                    public void foo() {
                        boolean b = true;
                        int x = b ? 1 : 0;
                    }
                }
            """
    )

    @Test
    fun typeArgumentsAfterCommaFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String,String> m = new HashMap<String,String>();
                        Test.<String,Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun typeArgumentsAfterCommaTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String,String> m = new HashMap<String,String>();
                        Test.<String,Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun typeArgumentsBeforeOpeningAngleBracketTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withBeforeOpeningAngleBracket(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map <String, String> m = new HashMap <String, String>();
                        Test. <String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun typeArgumentsBeforeOpeningAngleBracketFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withBeforeOpeningAngleBracket(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map <String, String> m = new HashMap <String, String>();
                        Test. <String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun typeArgumentsAfterClosingAngleBracketTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withAfterClosingAngleBracket(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer> bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun typeArgumentsAfterClosingAngleBracketFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeArguments(typeArguments.run {
                            withAfterClosingAngleBracket(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer> bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """,
            after = """
                import java.util.Map;
                import java.util.HashMap;

                class Test {
                    void foo() {
                        Map<String, String> m = new HashMap<String, String>();
                        Test.<String, Integer>bar();
                    }
                    static <A, B> void bar() {
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaTrueNewArrayInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1 , 2 , 3};
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseNewArrayInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1 , 2 , 3};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseNewArrayInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1,2,3};
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueNewArrayInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1,2,3};
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int[] arr = new int[]{1, 2, 3};
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaTrueMethodDeclArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void bar(int x, int y) {
                    }
                }
            """,
            after = """
                class Test {
                    void bar(int x , int y) {
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseMethodDeclArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void bar(int x , int y) {
                    }
                }
            """,
            after = """
                class Test {
                    void bar(int x, int y) {
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseMethodDeclArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void bar(int x, int y) {
                    }
                }
            """,
            after = """
                class Test {
                    void bar(int x,int y) {
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueMethodDeclArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void bar(int x,int y) {
                    }
                }
            """,
            after = """
                class Test {
                    void bar(int x, int y) {
                    }
                }
            """
    )

    val methodInvocationDependsOn
        get() = arrayOf("""
                class A {
                    void bar(int x, int y) {
                    }
                }
            """)

    @Test
    fun otherBeforeCommaTrueMethodInvocationParams(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            dependsOn = methodInvocationDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A().bar(1, 2);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A().bar(1 , 2);
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseMethodInvocationParams(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            dependsOn = methodInvocationDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A().bar(1 , 2);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A().bar(1, 2);
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseMethodInvocationParams(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            dependsOn = methodInvocationDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A().bar(1, 2);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A().bar(1,2);
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueMethodInvocationParams(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            dependsOn = methodInvocationDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A().bar(1,2);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A().bar(1, 2);
                    }
                }
            """
    )

    val newClassArgsDependsOn
        get() = arrayOf("""
            class A {
                A(String str, int num) {
                }
            }
        """)

    @Test
    fun otherBeforeCommaTrueNewClassArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            dependsOn = newClassArgsDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A("hello", 1);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A("hello" , 1);
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseNewClassArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            dependsOn = newClassArgsDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A("hello" , 1);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A("hello", 1);
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseNewClassArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            dependsOn = newClassArgsDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A("hello", 1);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A("hello",1);
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueNewClassArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            dependsOn = newClassArgsDependsOn,
            before = """
                class Test {
                    void foo() {
                        new A("hello",1);
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        new A("hello", 1);
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaTrueLambdaParameters(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                    }
                }
            """,
            after = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1 , str2) -> "Hello";
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseLambdaParameters(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1 , str2) -> "Hello";
                    }
                }
            """,
            after = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseLambdaParameters(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                    }
                }
            """,
            after = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1,str2) -> "Hello";
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueLambdaParameters(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1,str2) -> "Hello";
                    }
                }
            """,
            after = """
                import java.util.function.BiFunction;
                
                class Test {
                    void foo() {
                        BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaTrueForLoopUpdate(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++, x++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++ , x++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseForLoopUpdate(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++ , x++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++, x++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseForLoopUpdate(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++, x++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++,x++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueForLoopUpdate(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++,x++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int n = 0, x = 0; n < 100; n++, x++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaTrueEnumValueInitArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(true)
                        })
                    }))
            ).build(),
            before = """
                enum Test {
                    TEST1("str1", 1),
                    TEST2("str2", 2);
                    
                    Test(String str, int num) {
                    }
                }
            """,
            after = """
                enum Test {
                    TEST1("str1" , 1),
                    TEST2("str2" , 2);
                    
                    Test(String str , int num) {
                    }
                }
            """
    )

    @Test
    fun otherBeforeCommaFalseEnumValueInitArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeComma(false)
                        })
                    }))
            ).build(),
            before = """
                enum Test {
                    TEST1("str1" , 1),
                    TEST2("str2" , 2);
                    
                    Test(String str , int num) {
                    }
                }
            """,
            after = """
                enum Test {
                    TEST1("str1", 1),
                    TEST2("str2", 2);
                    
                    Test(String str, int num) {
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaFalseEnumValueInitArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(false)
                        })
                    }))
            ).build(),
            before = """
                enum Test {
                    TEST1("str1", 1),
                    TEST2("str2", 2);
                    
                    Test(String str, int num) {
                    }
                }
            """,
            after = """
                enum Test {
                    TEST1("str1",1),
                    TEST2("str2",2);
                    
                    Test(String str,int num) {
                    }
                }
            """
    )

    @Test
    fun otherAfterCommaTrueEnumValueInitArgs(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterComma(true)
                        })
                    }))
            ).build(),
            before = """
                enum Test {
                    TEST1("str1",1),
                    TEST2("str2",2);
                    
                    Test(String str,int num) {
                    }
                }
            """,
            after = """
                enum Test {
                    TEST1("str1", 1),
                    TEST2("str2", 2);
                    
                    Test(String str, int num) {
                    }
                }
            """
    )

    @Test
    fun otherBeforeForSemicolonTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeForSemicolon(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i = 0 ; i < 10 ; i++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherBeforeForSemicolonFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeForSemicolon(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i = 0 ; i < 10 ; i++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherAfterForSemicolonFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterForSemicolon(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i = 0;i < 10;i++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherAfterForSemicolonTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterForSemicolon(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i = 0;i < 10;i++) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i = 0; i < 10; i++) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherAfterTypeCastFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterTypeCast(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int i = (int) 0.0d;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int i = (int)0.0d;
                    }
                }
            """
    )

    @Test
    fun otherAfterTypeCastTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withAfterTypeCast(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        int i = (int)0.0d;
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int i = (int) 0.0d;
                    }
                }
            """
    )

    @Test
    fun otherBeforeColonInForEachFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeColonInForEach(false)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i: new int[]{1, 2, 3}) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherBeforeColonInForEachTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withBeforeColonInForEach(true)
                        })
                    }))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (int i: new int[]{1, 2, 3}) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (int i : new int[]{1, 2, 3}) {
                        }
                    }
                }
            """
    )

    @Test
    fun otherInsideOneLineEnumBracesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withInsideOneLineEnumBraces(true)
                        })
                    }))
            ).build(),
            before = """
                enum Test {}
            """,
            after = """
                enum Test { }
            """
    )

    @Test
    fun otherInsideOneLineEnumBracesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withOther(other.run {
                            withInsideOneLineEnumBraces(false)
                        })
                    }))
            ).build(),
            before = """
                enum Test { }
            """,
            after = """
                enum Test {}
            """
    )

    @Test
    fun typeParametersBeforeOpeningAngleBracketTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeParameters(typeParameters.run {
                            withBeforeOpeningAngleBracket(true)
                        })
                    }))
            ).build(),
            before = """
                class Test<T> {
                }
            """,
            after = """
                class Test <T> {
                }
            """
    )

    @Test
    fun typeParametersBeforeOpeningAngleBracketFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeParameters(typeParameters.run {
                            withBeforeOpeningAngleBracket(false)
                        })
                    }))
            ).build(),
            before = """
                class Test <T> {
                }
            """,
            after = """
                class Test<T> {
                }
            """
    )

    @Test
    fun typeParametersAroundTypeBoundsFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeParameters(typeParameters.run {
                            withAroundTypeBounds(false)
                        })
                    }))
            ).build(),
            before = """
                class Test<T extends Integer & Appendable> {
                }
            """,
            after = """
                class Test<T extends Integer&Appendable> {
                }
            """
    )

    @Test
    fun typeParametersAroundTypeBoundsTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    namedStyles(listOf(IntelliJ.spaces().run {
                        withTypeParameters(typeParameters.run {
                            withAroundTypeBounds(true)
                        })
                    }))
            ).build(),
            before = """
                class Test<T extends Integer&Appendable> {
                }
            """,
            after = """
                class Test<T extends Integer & Appendable> {
                }
            """
    )

    @Test
    fun noSpaceInitializerPadding(jp: JavaParser) = assertUnchanged(
            jp,
            before = """
            public class A {
                {
                    for (; i < j; i++, j--) { }
                }
            }
        """
    )

    @Test
    fun addSpaceToEmptyInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForInitializerPadStyle(
                true
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for (; i < j; i++, j--) { }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for ( ; i < j; i++, j--) { }
                }
            }
        """
    )

    @Test
    fun removeSpaceFromEmptyInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForInitializerPadStyle(
                false
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for ( ; i < j; i++, j--) { }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for (; i < j; i++, j--) { }
                }
            }
        """
    )

    @Test
    fun addSpaceToEmptyIterator(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForIteratorPadStyle(
                true
            )
        ))).build(),
        before = """
            public class A {
                {
                    for (int i = 0; i < 10;) { i++; }
                }
            }
        """,
            after = """
            public class A {
                {
                    for (int i = 0; i < 10; ) { i++; }
                }
            }
        """
    )

    @Test
    fun removeSpaceFromEmptyIterator(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForIteratorPadStyle(
                false
            )
        ))).build(),
        before = """
            public class A {
                {
                    for (int i = 0; i < 10; ) { i++; }
                }
            }
        """,
        after = """
            public class A {
                {
                    for (int i = 0; i < 10;) { i++; }
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1051")
    @Test
    fun preserveSpacePrecedingComment(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            @Deprecated("version" /* some comment */) 
            class Test {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1064")
    @Test
    fun preserveSpacePrecedingCommentInSpaceBefore(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            @Deprecated("version" /* some comment */) 
            class Test {
                void foo() {
                    List.of( // another comment
                        1,
                        2
                    );
                }
            }
        """
    )
}
