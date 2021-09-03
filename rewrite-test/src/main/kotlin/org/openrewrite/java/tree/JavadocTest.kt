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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
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
    fun indexOnly(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@index}
             * {@index
             */
            public class A {
                void method() {}
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/971")
    @Test
    fun indexNoDescription(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit,
        """
            /**
             * {@index term}
             * {@index term
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/971")
    @Test
    fun indexTermAndDescription(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit,
        """
            /**
             * {@index term description}
             * {@index term description
             */
            class Test {
            }
        """.trimIndent()
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
                 * @provides int
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
    fun whitespaceBeforeNonLeadingText(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @return <code>true</code>
                 * <code>false</code> non-leading text.
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Test
    fun whitespaceOnBlankLineBetweenBodyAndTags(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * Returns something.
                 * 
                 * @return true
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Test
    fun methodReferenceNoParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * {@linkplain Thread#interrupt}
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/942")
    @Test
    fun nullLiteral(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * {@literal null}.
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/941")
    @Test
    fun paramWithMultilineHtmlAttribute(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @param contentType <a href=
                 *                    "https://www...">
                 *                    label</a>
                 */
                boolean test(int contentType);
            }
        """.trimIndent()
    )

    @Test
    fun methodFound(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @see java.io.ByteArrayOutputStream#toString(String)
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/945")
    @Test
    fun methodNotFound(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @see Math#cosine(double)
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/944")
    @Test
    fun typeNotFound(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * {@link SymbolThatCannotBeFound}
                 * @see Mathy#cos(double)
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Test
    fun multipleReferenceParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            class Test {
                /**
                 * {@link ListenerUtils#getExceptionFromHeader(ConsumerRecord, String, LogAccessor)}
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/941")
    @Test
    fun seeWithMultilineAttribute(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @see <a href="https://www...">
                 *            label</a>
                 */
                boolean test();
            }
        """.trimIndent()
    )

    @Test
    fun consecutiveLineBreaks(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            class Test {
                /** 
                 * @param oboFile the file to be parsed
            
                 * @return the ontology represented as a BioJava ontology file
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/957")
    @Test
    fun commentMissingMultipleAsterisks(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            class Test {
                /** 
                 * JavaDoc
                    1st new line.
                    2nd new line.
                 * text
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/963")
    @Test
    fun blankLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@link}
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/968")
    @Test
    fun missingBracket(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@link missing.bracket
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/964")
    @Test
    fun constructorLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            class Test {
                /**
                 * {@link Constructor()}
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/965")
    @Test
    fun emptyJavadoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /***/
            class Test {
                /**
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/967")
    @Test
    fun multilineLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * {@link
             * multiline}.
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun multilineAttribute(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            /**
             * <a href="
             * https://...html">
             * label</a>.
             */
            class Test {
            }
        """.trimIndent()
    )

    @Disabled
    @Test
    fun lineBreakInParam(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.CompilationUnit, """
            interface Test {
                /**
                 * @param <
                 *   T> t hi
                 */
                <T> boolean test();
            }
        """.trimIndent()
    )
}
