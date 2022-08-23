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
@file:Suppress("RedundantThrows", "JUnitMalformedDeclaration", "resource")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface UnnecessaryThrowsTest : JavaRecipeTest, RewriteTest {
    override val recipe: Recipe
        get() = UnnecessaryThrows()

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UnnecessaryThrows())
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2144")
    @Test
    fun genericException(jp: JavaParser.Builder<*, *>) = rewriteRun(
            { spec -> spec.parser(jp) },
            java("""
                class Test {
                    public <E extends Exception> void accept(Class<E> e) throws E {
                    }
                }
            """)
    )

    @Test
    fun unnecessaryThrows(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.io.FileInputStream;
            import java.io.FileNotFoundException;
            import java.io.IOException;
            import java.io.UncheckedIOException;
            class Test {
                private void test() throws FileNotFoundException, UncheckedIOException {
                }

                void test() throws IOException, UncheckedIOException {
                    new FileInputStream("test");
                }
            }
        """,
        after = """
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.io.UncheckedIOException;
            class Test {
                private void test() throws UncheckedIOException {
                }
            
                void test() throws IOException, UncheckedIOException {
                    new FileInputStream("test");
                }
            }
        """
    )

    @Suppress("EmptyTryBlock")
    @Issue("https://github.com/openrewrite/rewrite/issues/631")
    @Test
    fun necessaryThrowsFromCloseable(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            import java.net.URL;
            import java.net.URLClassLoader;
            
            class Test {
                public void closeable() throws IOException {
                    // URLClassLoader implements Closeable and throws IOException from its close() method
                    try (URLClassLoader cl = new URLClassLoader(new URL[0])) {
                    }
                }
            }
        """
    )

    @Test
    fun necessaryThrows(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            
            class Test {
                
                void test() throws IOException {
                    throw new IOException();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/519")
    @Test
    fun interfaces(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            
            interface Test {
                void test() throws IOException;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/519")
    @Test
    fun abstractMethods(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            
            abstract class Test {
                abstract void test() throws IOException;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1059")
    @Test
    fun necessaryThrowsFromStaticMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import javax.xml.datatype.DatatypeFactory;
            
            class Test {
                void test() throws Exception {
                    DatatypeFactory.newInstance();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/897")
    @Test
    fun necessaryThrowsOnInterfaceWithExplicitOverride(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            public interface Foo {
                void bar() throws Exception;
            }
        """),
        before = """
            public class FooImpl implements Foo {
                @Override
                public void bar() throws Exception {
                    // no-op
                }
            }
        """
    )

    @Test
    fun necessaryThrowsOnInterfaceWithImplicitOverride(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            public interface Foo {
                void bar() throws Exception;
            }
        """),
        before = """
            public class FooImpl implements Foo {
                public void bar() throws Exception {
                    // no-op
                }
            }
        """
    )

    @Test
    fun doNotRemoveDocumentedExceptions(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class ParentFoo {
                /**
                 * @throws Exception Throws an exception
                 */
                public void bar() throws Exception { // this throws should not be removed
                }
            }
            
            class Foo extends ParentFoo {
                @Override
                public void bar() throws Exception {
                    // no-op
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1298")
    @Test
    fun doNotRemoveExceptionCoveringOtherExceptions(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package com.yourorg;
            
            import java.io.IOException;
            
            class A {
                void foo() throws Exception {
                    throw new IOException("");
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2105")
    @Test
    fun `prevent transformation if any thrown exception has a null or unknown type`(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            package com.yourorg;
            
            import java.io.IOException;
            
            class A {
                void foo() throws ExceptionWithUnknownType {
                    someUnknownMethodInvocation();
                }
            }
        """
    )
}
