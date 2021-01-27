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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.TabsAndIndentsStyle
import org.openrewrite.style.NamedStyles

interface TabsAndIndentsTest : RecipeTest {
    override val recipe: Recipe
        get() = TabsAndIndents()

    fun tabsAndIndents(with: TabsAndIndentsStyle.() -> TabsAndIndentsStyle = { this }) = listOf(
        NamedStyles(
            "test", listOf(
                IntelliJ.tabsAndIndents().run { with(this) })
        )
    )

    @Test
    fun methodChain(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
            class Test {
                void method(Test t) {
                    this
                      .method(
                        t
                      );
                }
            }
        """
    )

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @Test
    fun tabsAndIndents(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
            public int[] X = new int[]{1, 3, 5, 7, 9, 11};

            public void test(boolean a, int x, int y, int z) {
            label1:
            do {
            try {
            if (x > 0) {
            int someVariable = a ? x : y;
            int anotherVariable = a ? x : y;
            } else if (x < 0) {
            int someVariable = (y + z);
            someVariable = x = x + y;
            } else {
            label2:
            for (int i = 0; i < 5; i++) doSomething(i);
            }
            switch (a) {
            case 0:
            doCase0();
            break;
            default:
            doDefault();
            }
            } catch (Exception e) {
            processException(e.getMessage(), x + y, z, a);
            } finally {
            processFinally();
            }
            }
            while (true);

            if (2 < 3) return;
            if (3 < 4) return;
            do {
            x++;
            }
            while (x < 10000);
            while (x < 50000) x++;
            for (int i = 0; i < 5; i++) System.out.println(i);
            }

            private class InnerClass implements I1, I2 {
            public void bar() throws E1, E2 {
            }
            }
            }
        """,
        after = """
            public class Test {
                public int[] X = new int[]{1, 3, 5, 7, 9, 11};

                public void test(boolean a, int x, int y, int z) {
                    label1:
                    do {
                        try {
                            if (x > 0) {
                                int someVariable = a ? x : y;
                                int anotherVariable = a ? x : y;
                            } else if (x < 0) {
                                int someVariable = (y + z);
                                someVariable = x = x + y;
                            } else {
                                label2:
                                for (int i = 0; i < 5; i++) doSomething(i);
                            }
                            switch (a) {
                                case 0:
                                    doCase0();
                                    break;
                                default:
                                    doDefault();
                            }
                        } catch (Exception e) {
                            processException(e.getMessage(), x + y, z, a);
                        } finally {
                            processFinally();
                        }
                    }
                    while (true);

                    if (2 < 3) return;
                    if (3 < 4) return;
                    do {
                        x++;
                    }
                    while (x < 10000);
                    while (x < 50000) x++;
                    for (int i = 0; i < 5; i++) System.out.println(i);
                }

                private class InnerClass implements I1, I2 {
                    public void bar() throws E1, E2 {
                    }
                }
            }
        """
    )

    @Test
    fun tryCatchFinally(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
            public void test(boolean a, int x, int y) {
            try {
            int someVariable = a ? x : y;
            } catch (Exception e) {
            e.printStackTrace();
            } finally {
            a = false;
            }
            }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
                        int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """
    )

    @Test
    fun doWhile(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
            public void test() {
            do {
            }
            while(true);

            labeled: do {
            }
            while(false);
            }
            }
        """,
        after = """
            public class Test {
                public void test() {
                    do {
                    }
                    while(true);

                    labeled: do {
                    }
                    while(false);
                }
            }
        """
    )

    @Test
    fun elseBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
            public void test(boolean a, int x, int y, int z) {
            if (x > 0) {
            } else if (x < 0) {
            y += z;
            }
            }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y, int z) {
                    if (x > 0) {
                    } else if (x < 0) {
                        y += z;
                    }
                }
            }
        """
    )

    @Test
    @Disabled
    fun forLoop(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
            public class Test {
                public void test() {
                int m = 0;
                int n = 0;
                for (
                 int i = 0
                 ;
                 i < 5
                 ;
                 i++, m++, n++
                );
                for (int i = 0;
                 i < 5;
                 i++, m++, n++);
                labeled: for (int i = 0;
                 i < 5;
                 i++, m++, n++);
                }
            }
        """,
        after = """
            public class Test {
                public void test() {
                    int m = 0;
                    int n = 0;
                    for (
                      int i = 0
                      ;
                      i < 5
                      ;
                      i++, m++, n++
                    );
                    for (int i = 0;
                         i < 5;
                         i++, m++, n++);
                    labeled: for (int i = 0;
                                  i < 5;
                                  i++, m++, n++);
                }
            }
        """
    )

    @Test
    fun methodDeclaration(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
            public class Test {
                public void test(int a,
                                 int b) {}

                public void test2(
                  int a,
                  int b) {}
            }
        """
    )

    @Test
    fun lineComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            // this is a comment
            public void method() {}
            }
        """,
        after = """
            public class A {
                // this is a comment
                public void method() {}
            }
        """
    )

    @Test
    fun blockComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /* this is a comment
               that extends onto another line */
            public void method() {}
            }
        """,
        after = """
            public class A {
                /* this is a comment
                   that extends onto another line */
                public void method() {}
            }
        """
    )

    @Test
    fun annotations(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            @Deprecated
            @SuppressWarnings("ALL")
            public class A {
            @Deprecated
            @SuppressWarnings("ALL")
                class B {
                }
            }
        """,
        after = """
            @Deprecated
            @SuppressWarnings("ALL")
            public class A {
                @Deprecated
                @SuppressWarnings("ALL")
                class B {
                }
            }
        """
    )

    @Test
    fun javadoc(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /**
             * This is a javadoc
             */
            public void method() {}
            }
        """,
        after = """
            public class A {
                /**
                 * This is a javadoc
                 */
                public void method() {}
            }
        """
    )

    @Test
    fun tabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        // TIP: turn on "Show Whitespaces" in the IDE to see this test clearly
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class A {
                public void method() {
                int n = 0;
                }
            }
        """,
        after = """
            public class A {
                public void method() {
                	int n = 0;
                }
            }
        """
    )

    @Test
    fun shiftRight(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
                int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
                        int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """
    )

    @Test
    fun shiftRightTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
            	int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
            			int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """
    )

    @Test
    fun shiftLeft(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
                                            int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
                        int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """
    )

    @Test
    fun shiftLeftTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
            				int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """,
        after = """
            public class Test {
                public void test(boolean a, int x, int y) {
                    try {
            			int someVariable = a ? x : y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        a = false;
                    }
                }
            }
        """
    )

    @Test
    fun annotationOnSameLine(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test { 
                @Bean int method() {
                    return 1;
                }
            }
        """
    )

    @Test
    fun newClassAsMethodArgument(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                Test(String s, int m) {
                }
            
                void method(Test t) {
                    method(new Test("hello" +
                            "world",
                            1));
                }
            }
        """
    )

    @Test
    fun methodArgumentsThatDontStartOnNewLine(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.File;
            class Test {
                void method(int n, File f, int m, int l) {
                    method(n, new File(
                                    "test"
                            ),
                            m,
                            l);
                }
            
                void method2(int n, File f, int m) {
                    method(n, new File(
                                    "test"
                            ), m,
                            0);
                }
            
                void method3(int n, File f) {
                    method2(n, new File(
                            "test"
                    ), 0);
                }
            
                void method4(int n) {
                    method3(n, new File(
                            "test"
                    ));
                }
            }
        """
    )

    @Test
    fun lambda(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Supplier;
            public class Test {
                public void method(int n) {
                    Supplier<Integer> ns = () ->
                        n;
                }
            }
        """,
        after = """
            import java.util.function.Supplier;
            public class Test {
                public void method(int n) {
                    Supplier<Integer> ns = () ->
                            n;
                }
            }
        """
    )

    @Test
    fun enums(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            enum Scope {
                None, // the root of a resolution tree
                Compile,
            }
        """
    )

    @Test
    fun twoThrows(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
           import java.io.IOException;
           class Test {
               void method() throws IOException,
                       Exception {
               }
               
               void method2()
                       throws IOException,
                       Exception {
               }
           }
        """
    )

    @Test
    fun twoTypeParameters(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test<A,
                    B> {
            }
        """,
        dependsOn = arrayOf("interface A {}", "interface B{}")
    )

    @Test
    fun twoImplements(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test implements A,
                    B {
            }
        """,
        dependsOn = arrayOf("interface A {}", "interface B{}")
    )

    @Test
    fun fieldsWhereClassHasAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            @Deprecated
            class Test {
                String groupId;
                String artifactId;
            }
        """
    )

    @Test
    fun methodWithAnnotation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.build(),
        before = """
            class Test {
                @Deprecated
                @SuppressWarnings("all")
            String getOnError() {
                    return "uh oh";
                }
            }
        """,
        after = """
            class Test {
                @Deprecated
                @SuppressWarnings("all")
                String getOnError() {
                    return "uh oh";
                }
            }
        """
    )

    @Test
    fun containers(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.io.ByteArrayInputStream;
            import java.io.InputStream;
            import java.io.Serializable;
            @Deprecated
            (since = "1.0")
            public
            class
            Test
            <T
            extends Object>
            implements
            Serializable {
                Test method
                ()
                throws Exception {
                    try
                    (InputStream is = new ByteArrayInputStream(new byte[0])) {}
                    int n[] = 
                    {0};
                    switch (1) {
                    case 1:
                    n
                    [0]++;
                    }
                    return new Test
                    ();
                }
            }
        """,
        after = """
            import java.io.ByteArrayInputStream;
            import java.io.InputStream;
            import java.io.Serializable;
            @Deprecated
                    (since = "1.0")
            public
            class
            Test
                    <T
                            extends Object>
                    implements
                    Serializable {
                Test method
                        ()
                        throws Exception {
                    try
                            (InputStream is = new ByteArrayInputStream(new byte[0])) {}
                    int n[] = 
                            {0};
                    switch (1) {
                        case 1:
                            n
                                    [0]++;
                    }
                    return new Test
                            ();
                }
            }
        """
    )

    @Test
    fun methodInvocations(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                Test method(int n) {
                    return method(n)
                            .method(n)
                            .method(n);
                }
            
                Test method2() {
                    return method2().
                            method2().
                            method2();
                }
            }
        """
    )

    @Test
    fun ternaries(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                public Test method(int n) {
                    return n > 0 ?
                        this :
                        method(n).method(n);
                }
            }
        """,
        after = """
            public class Test {
                public Test method(int n) {
                    return n > 0 ?
                            this :
                            method(n).method(n);
                }
            }
        """
    )

    @Test
    fun newClassAsArgument(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.File;
            class Test {
                void method(int m, File f, File f2) {
                    method(m, new File(
                                    "test"
                            ),
                            new File("test",
                                    "test"
                            ));
                }
            }
        """
    )

    @Test
    fun variableWithAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class Test {
                @Deprecated
                final Scope scope;
            
                @Deprecated
                String classifier;
            }
        """
    )

    @Test
    fun lambdaMethodParameter2(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Function;

            abstract class Test {
                abstract Test a(Function<Test, Test> f);
                abstract Test b(Function<Test, Test> f);
                abstract Test c(Function<Test, Test> f);

                Test method(Function<Test, Test> f) {
                    return a(f)
                            .b(
                                    t ->
                                            c(f)
                            );
                }
            }
        """.trimIndent()
    )

    @Test
    fun lambdaMethodParameter(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Function;
            abstract class Test {
                abstract Test a(Function<Test, Test> f);
                abstract Test b(Function<Test, Test> f);
                abstract Test c(Function<Test, Test> f);
                
                Test method(Function<Test, Test> f) {
                    return a(f)
                            .b(t ->
                                    c(f)
                            );
                }
            }
        """
    )

    @Test
    fun failure1(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
                public class Test {
                    public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system,
                                                                                            @Nullable File localRepositoryDir) {
                        DefaultRepositorySystemSession repositorySystemSession = org.apache.maven.repository.internal.MavenRepositorySystemUtils
                                .newSession();
                        repositorySystemSession.setDependencySelector(
                                new AndDependencySelector(
                                        new ExclusionDependencySelector(),
                                        new ScopeDependencySelector(emptyList(), Arrays.asList("provided", "test")),
                                        new OptionalDependencySelector()
                                )
                        );
                        return repositorySystemSession;
                    }
                }
            """
    )

    @Test
    fun methodInvocationsNotContinuationIndentedWhenPartOfBinaryExpression(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.stream.Stream;
            public class Test {        
                boolean b;
                public Stream<Test> method() {
                    if (b && method()
                            .anyMatch(t -> b ||
                                    b
                            )) {
                        // do nothing
                    }
                    return Stream.of(this);
                }
            }
        """
    )

    @Test
    fun punctuation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
            import java.util.function.Function;
            public class Test {
            int X[];
            public int plus(int x) {
                return 0;
            }
            public void test(boolean a, int x, int y) {
            Function<Integer, Integer> op = this
            ::
            plus;
            if (x
            >
            0) {
            int someVariable = a ?
            x :
            y;
            int anotherVariable = a
            ?
            x
            :
            y;
            }
            x
            ++;
            X
            [
            1
            ]
            =
            0;
            }
            }
        """,
        after = """
            import java.util.function.Function;
            public class Test {
                int X[];
                public int plus(int x) {
                    return 0;
                }
                public void test(boolean a, int x, int y) {
                    Function<Integer, Integer> op = this
                      ::
                      plus;
                    if (x
                      >
                      0) {
                        int someVariable = a ?
                          x :
                          y;
                        int anotherVariable = a
                          ?
                          x
                          :
                          y;
                    }
                    x
                      ++;
                    X
                      [
                      1
                      ]
                      =
                      0;
                }
            }
        """
    )

    @Test
    fun newClass(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                Test(Test t) {}
                Test() {}
                void method(Test t) {
                    method(
                        new Test(
                            new Test()
                        )
                    );
                }
            }
        """,
        after = """
            class Test {
                Test(Test t) {}
                Test() {}
                void method(Test t) {
                    method(
                            new Test(
                                    new Test()
                            )
                    );
                }
            }
        """
    )
}
