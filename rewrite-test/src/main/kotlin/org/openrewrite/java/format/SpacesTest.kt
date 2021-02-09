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
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.style.NamedStyles

interface SpacesTest : RecipeTest {
    override val recipe: Recipe?
        get() = Spaces()

    @Test
    fun beforeParensMethodDeclarationTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("Test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodDeclaration(true)
                        })
                    })))
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
                    listOf(NamedStyles("Test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodDeclaration(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodCall(true)
                        })
                    })))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        foo();
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        foo ();
                    }
                }
            """
    )

    @Test
    fun beforeParensMethodCallFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodCall(false)
                        })
                    })))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        foo ();
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        foo();
                    }
                }
            """
    )

    @Test
    fun beforeParensIfParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withIfParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withIfParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withForParentheses(false)
                        })
                    })))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for (;;) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for(;;) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensForParenthesesTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withForParentheses(true)
                        })
                    })))
            ).build(),
            before = """
                class Test {
                    void foo() {
                        for(;;) {
                        }
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        for (;;) {
                        }
                    }
                }
            """
    )

    @Test
    fun beforeParensWhileParenthesesFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withWhileParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withWhileParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSwitchParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSwitchParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withTryParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withTryParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withCatchParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withCatchParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSynchronizedParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withSynchronizedParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withAnnotationParameters(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withAnnotationParameters(false)
                        })
                    })))
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

    @Issue("https://github.com/openrewrite/rewrite/issues/234")
    @Test
    fun withinAnnotationParametersSpaces(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.withAnnotationParentheses(true))
                    })))
            ).build(),
            dependsOn = arrayOf("""
                @interface Foo {
                    String[] exclude() default {};
                    boolean callSuper() default false;
                }
            """),
            before = """
                @Foo(exclude = {"this","that"},callSuper=false)
                class Test {
                }
            """,
            after = """
                @Foo(exclude = {"this", "that"}, callSuper = false)
                class Test {
                }
            """
    )

    @Test
    fun aroundOperatorsAssignmentFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAssignment(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAssignment(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLogical(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLogical(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withEquality(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withEquality(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withRelational(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withRelational(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withBitwise(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withBitwise(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAdditive(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withAdditive(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMultiplicative(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMultiplicative(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withShift(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withShift(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withUnary(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withUnary(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLambdaArrow(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withLambdaArrow(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMethodReferenceDoubleColon(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withAroundOperators(aroundOperators.run {
                            withMethodReferenceDoubleColon(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withClassLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withClassLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withMethodLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withMethodLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withIfLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withIfLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withElseLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withElseLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withForLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withForLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withWhileLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withWhileLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withDoLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withDoLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSwitchLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSwitchLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withTryLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withTryLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withCatchLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withCatchLeftBrace(true)
                        })
                    })))
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

    @Test
    fun beforeLeftBraceFinallyLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withFinallyLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withFinallyLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSynchronizedLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withSynchronizedLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withArrayInitializerLeftBrace(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withArrayInitializerLeftBrace(false)
                        })
                    })))
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

    @Test
    fun beforeLeftBraceAnnotationArrayInitializerLeftBraceTrue(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withAnnotationArrayInitializerLeftBrace(true)
                        })
                    })))
            ).build(),
            before = """
                @SuppressWarnings({"ALL"})
                class Test {
                }
            """,
            after = """
                @SuppressWarnings( {"ALL"})
                class Test {
                }
            """
    )

    @Test
    fun beforeLeftBraceAnnotationArrayInitializerLeftBraceFalse(jp: JavaParser.Builder<*, *>) = assertChanged(
            parser = jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeLeftBrace(beforeLeftBrace.run {
                            withAnnotationArrayInitializerLeftBrace(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withWhileKeyword(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withWhileKeyword(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withCatchKeyword(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withCatchKeyword(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withFinallyKeyword(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeKeywords(beforeKeywords.run {
                            withFinallyKeyword(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCodeBraces(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCodeBraces(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withBrackets(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withBrackets(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withArrayInitializerBraces(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withArrayInitializerBraces(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyArrayInitializerBraces(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyArrayInitializerBraces(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withGroupingParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withGroupingParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodDeclarationParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodDeclarationParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodDeclarationParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodDeclarationParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodCallParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withMethodCallParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodCallParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withEmptyMethodCallParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withIfParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withIfParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withForParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withForParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withWhileParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withWhileParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSwitchParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSwitchParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTryParentheses(true)
                        })
                    })))
            ).build(),
            dependsOn = arrayOf("""
                class MyResource implements Closeable {
                }
            """),
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTryParentheses(false)
                        })
                    })))
            ).build(),
            dependsOn = arrayOf("""
                class MyResource implements Closeable {
                }
            """),
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCatchParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withCatchParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSynchronizedParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withSynchronizedParentheses(false)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTypeCastParentheses(true)
                        })
                    })))
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
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withWithin(within.run {
                            withTypeCastParentheses(false)
                        })
                    })))
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
}
