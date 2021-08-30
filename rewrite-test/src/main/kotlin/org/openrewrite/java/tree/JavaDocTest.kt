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

interface JavaDocTest : JavaTreeTest {

    @Test
    fun visitAuthor(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitDeprecated(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitDocRoot(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitException(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitHidden(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitIndex(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitInheritDoc(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitLiteral(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitLink(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitParam(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitProvide(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun visitReturn(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseSee(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseSerial(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseSerialData(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseSince(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseSummary(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseValue(jp: JavaParser) = assertParsePrintAndProcess(
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
    fun parseVersion(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * @version 1.0.0
             */
            public class A {
                void method() {}
            }
        """
    )
}
