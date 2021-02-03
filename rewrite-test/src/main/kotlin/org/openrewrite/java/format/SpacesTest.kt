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
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.style.NamedStyles

interface SpacesTest : RecipeTest {
    override val recipe: Recipe?
        get() = Spaces()

    val dependsOn: Array<String>
        get() = arrayOf(
                """
                    class MyResource implements AutoCloseable {
                        public void close() {
                        }
                    }
                """
        )

    val testCode: String
        get() = """
            @SuppressWarnings({"ALL"})
            public class A {
                void bar() {}
                void foo(int arg) {
                    Runnable r = () -> {};
                    Runnable r1 = this::bar;
                    if (true) {
                        foo(1);
                    } else {
                        foo(2);
                    }
                    int j = 0;
                    for (int i = 0; i < 10 || j > 0; i++) {
                        j += i;
                    }
                    int[] arr = new int[]{1, 3, 5, 6, 7, 87, 1213, 2};
                    for (int e : arr) {
                        j += e;
                    }
                    int[] arr2 = new int[]{};
                    int elem = arr[j];
                    int x;
                    while (j < 1000 && x > 0) {
                        j = j + 1;
                    }
                    do {
                        j = j + 1;
                    } while (j < 2000);
                    switch (j) {
                        case 1:
                        default:
                    }
                    try (MyResource res1 = new MyResource(); MyResource res2 = null) {
                    } catch (Exception e) {
                    } finally {
                    }
                    Object o = new Object();
                    synchronized (o) {
                    }
                    if (x == 0) {
                    }
                    if (j != 0) {
                    }
                    if (x <= 0) {
                    }
                    if (j >= 0) {
                    }
                    x = x << 2;
                    x = x >> 2;
                    x = x >>> 2;
                    x = x | 2;
                    x = x & 2;
                    x = x ^ 2;
                    x = x + 1;
                    x = x - 1;
                    x = x * 2;
                    x = x / 2;
                    x = x % 2;
                    boolean b;
                    b = !b;
                    x = -x;
                    x = +x;
                    x++;
                    ++x;
                    x--;
                    --x;
                    x += (x + 1);
                }
            }
            
            @SuppressWarnings({})
            public interface I {}
            
            public class C {}
        """

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
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x=0;
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
                    }
                }
            """,
            after = """
                class Test {
                    void foo() {
                        int x = 0;
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
    fun beforeParens(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(
                    listOf(NamedStyles("test", listOf(IntelliJ.spaces().run {
                        withBeforeParentheses(beforeParentheses.run {
                            withMethodDeclaration(true)
                                    .withMethodCall(true)
                                    .withIfParentheses(false)
                                    .withForParentheses(false)
                                    .withWhileParentheses(false)
                                    .withSwitchParentheses(false)
                                    .withTryParentheses(false)
                                    .withCatchParentheses(false)
                                    .withSynchronizedParentheses(false)
                                    .withAnnotationParameters(true)
                        }).withAroundOperators(aroundOperators.run {
                            withAssignment(false)
                                    .withLogical(false)
                                    .withEquality(false)
                                    .withRelational(false)
                                    .withBitwise(false)
                                    .withAdditive(false)
                                    .withMultiplicative(false)
                                    .withShift(false)
                                    .withUnary(true)
                                    .withLambdaArrow(false)
                                    .withMethodReferenceDoubleColon(true)
                        }).withBeforeLeftBrace(beforeLeftBrace.run {
                            withClassLeftBrace(false)
                                    .withMethodLeftBrace(false)
                                    .withIfLeftBrace(false)
                                    .withElseLeftBrace(false)
                                    .withForLeftBrace(false)
                                    .withWhileLeftBrace(false)
                                    .withDoLeftBrace(false)
                                    .withSwitchLeftBrace(false)
                                    .withTryLeftBrace(false)
                                    .withCatchLeftBrace(false)
                                    .withFinallyLeftBrace(false)
                                    .withSynchronizedLeftBrace(false)
                                    .withArrayInitializerLeftBrace(true)
                                    .withAnnotationArrayInitializerLeftBrace(true)
                        }).withBeforeKeywords(beforeKeywords.run {
                            withElseKeyword(false)
                                    .withWhileKeyword(false)
                                    .withCatchKeyword(false)
                                    .withFinallyKeyword(false)
                        }).withWithin(within.run {
                            withCodeBraces(true)
                                    .withBrackets(true)
                                    .withArrayInitializerBraces(true)
                                    .withEmptyArrayInitializerBraces(true)
                                    .withGroupingParentheses(true)
                        })
                    })))
            ).build(),
            dependsOn = dependsOn,
            before = testCode,
            after = /* THE HORROR */ """
            @SuppressWarnings ( { "ALL" })
            public class A{
                void bar (){}
                void foo (int arg){
                    Runnable r=()->{};
                    Runnable r1=this :: bar;
                    if(true){
                        foo (1);
                    }else{
                        foo (2);
                    }
                    int j=0;
                    for(int i=0; i<10||j>0; i ++){
                        j+=i;
                    }
                    int[] arr=new int[] { 1, 3, 5, 6, 7, 87, 1213, 2 };
                    for(int e : arr){
                        j+=e;
                    }
                    int[] arr2=new int[] { };
                    int elem=arr[ j ];
                    int x;
                    while(j<1000&&x>0){
                        j=j+1;
                    }
                    do{
                        j=j+1;
                    }while(j<2000);
                    switch(j){
                        case 1:
                        default:
                    }
                    try(MyResource res1 = new MyResource(); MyResource res2 = null){
                    }catch(Exception e){
                    }finally{
                    }
                    Object o=new Object();
                    synchronized(o){
                    }
                    if(x==0){
                    }
                    if(j!=0){
                    }
                    if(x<=0){
                    }
                    if(j>=0){
                    }
                    x=x<<2;
                    x=x>>2;
                    x=x>>>2;
                    x=x|2;
                    x=x&2;
                    x=x^2;
                    x=x+1;
                    x=x-1;
                    x=x*2;
                    x=x/2;
                    x=x%2;
                    boolean b;
                    b=! b;
                    x=- x;
                    x=+ x;
                    x ++;
                    ++ x;
                    x --;
                    -- x;
                    x+=( x+1 );
                }
            }

            @SuppressWarnings ( { })
            public interface I{ }
            
            public class C{ }
        """
    )

    @Test
    fun unchanged(jp: JavaParser.Builder<*, *>) = assertUnchanged(
            jp.styles(listOf(NamedStyles("testspaces", listOf(IntelliJ.spaces())))).build(),
            dependsOn = dependsOn,
            before = testCode
    )
}
