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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest

interface JavadocTest : JavaTreeTest {

    @Test
    fun author(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @author name
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun deprecated(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @deprecated reason
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun docRoot(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @docRoot
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun exception(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @exception ex
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun hidden(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @hidden value
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun index(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@index}
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun inheritDoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@inheritDoc}
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun literal(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            public class A {
                /**
                 * @literal
                 */
                void method() {}
            }
        """
    )

    @Test
    fun link(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@link java.util.List}
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun param(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            public class A {
                /**
                 * @param val
                 */
                void method(int val) {}
            }
        """
    )

    @Test
    fun provide(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            public class A {
                /**
                 * @provides
                 */
                void method(int val) {}
            }
        """
    )

    @Test
    fun returnTag(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            public class A {
                /**
                 * @return id
                 */
                int method(int val) {}
            }
        """
    )

    @Test
    fun see(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @see "Create link via quotes"
             * @see java.lang.Comparable#compareTo(Object) label
             * @see <a href="https://link.here">label</a>
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun serial(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @serial
             */
            public class A {
                void method() {}
            }
        """
    )


    @Test
    fun serialData(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @serialData
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun since(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @since
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun summary(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@summary}
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun value(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @value
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun version(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @version 1.0.0
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun descriptionOnNewLine(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            public class Test {
                 /**
                  * @param name
                  *            a name
                  */
                void test(String name) {
                }
            }
        """
    )

    @Test
    fun multipleLinesBeforeTag(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
        /**
         * Note
         *
         * @see CoreJackson2Module
         */
        public class Test {
        }
    """
    )

    @Test
    fun multipleLineErroneous(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @see this
             * or that
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun codeOnNextLine(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * Returns something.
                 * 
                 * @return <code>true</code>
                 * <code>false</code> otherwise.
                 */
                boolean test();
            }
        """.trimIndent()
    )
}
