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
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface JavadocTest : JavaTreeTest {

    @Test
    fun javadocs(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.util.List;
            
            /**
             *   {@link List#add(Object) } refers to import
             * @param Something that spans
             * multiple lines.
             */
            class Test {
                /**   the position of the first body element or tag is relative to the index beginning after the last contiguous whitespace following '**' */
                Integer n;
            
                /**
                 * {@link int}
                 */
                class Inner {
                    Integer n;
                    
                    /**
                     * {@link #n} refers to Inner
                     */
                    void test() {
                    }
                }
            }
        """.trimIndent()
    )

    @Test
    fun allBlank(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun allBlankMultiline(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             *
             *
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun singleLineParam(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   @param <T> t */
            class Test<T> {}
        """.trimIndent()
    )

    @Test
    fun noMarginJavadocFirstLineTrailingWhitespace(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   
               {@link int}
            */
            class Test {}
        """.trimIndent()
    )

    @Test
    fun leftMargin(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**   
                   {@link int}
                */
                String s;
            }
        """.trimIndent()
    )

    @Test
    fun singleLineJavadoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   {@link int} */
            class Test {}
        """.trimIndent()
    )

    @Test
    fun starMarginWithFirstLineLeadingSpace(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   
              *       Line 1
              */
            class Test<T> {}
        """.trimIndent()
    )

    @Test
    fun singleLineJavadocText(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   test */
            class Test {}
        """.trimIndent()
    )

    @Test
    fun noMarginJavadoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
               {@link int}
            */
            class Test {}
        """.trimIndent()
    )

    @Test
    fun javadocReturn(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * Message
                 * @return Line 1
                 * Line 2
                 */
                int test() {
                }
            }
        """.trimIndent()
    )

    @Test
    fun paramWithoutMargin(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            public class Test {
                /** Text
                 @return No margin
                 */
                public int test() {
                    return 0;
                }
            }
        """.trimIndent()
    )

    @Test
    fun author(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
            /**
             * See the <a href="{@docRoot}/copyright.html">Copyright</a>.
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun exception(jp: JavaParser) = assertParsePrintAndProcess(
        jp,CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
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
        jp,
        CompilationUnit,
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
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * {@inheritDoc}
                 * @return {@inheritDoc}
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Test
    fun literal(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            public class A {
                /**
                 * @literal
                 */
                void method() {}
            }
        """
    )

    @Test
    fun fullyQualifiedLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * {@link java.util.List}
             */
            public class A {
                void method() {}
            }
        """
    )

    @Test
    fun fullyQualifiedMethodLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * {@link java.util.List#add(Object) }
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun fieldLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.nio.charset.StandardCharsets;
            
            /**
             *   {@link StandardCharsets#UTF_8 }
             *   {@linkplain StandardCharsets#UTF_8 }
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun thisFieldLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             *   {@link #n }
             */
            class Test {
                int n;
            }
        """.trimIndent()
    )

    @Test
    fun thisMethodLink(jp: JavaParser.Builder<*, *>) = assertParsePrintAndProcess(
        jp.logCompilationWarningsAndErrors(true).build(),
        CompilationUnit,
        """
            /**
             *   {@link #test() }
             */
            class Test {
                void test() {}
            }
        """.trimIndent()
    )

    @Test
    fun primitiveLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * Line 1
             * Line 2
             * {@link int}
             * @param <T> t
             */
            class Test<T> {}
        """.trimIndent()
    )

    @Test
    fun param(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
            /** @since 1.0 */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun summary(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** {@summary test description } */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun value(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * The value of this constant is {@value}.
                 */
                public static final String SCRIPT_START = "<script>";
            
                /**
                 * {@value Test#SCRIPT_START}
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    @Test
    fun version(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
    fun erroneous(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** {@version this is an erroneous tag } */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun multipleLineErroneous(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * @see this
             * or that
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun code(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** {@code int n = 1; } */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun whitespaceBeforeNonLeadingText(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
            /**
             * {@link
             * multiline}.
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun doctype(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** <!doctype text > test */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun htmlComment(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** <!-- comment --> */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun htmlEntity(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** &amp; &amp ; &#12; */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun multilineAttribute(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
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
        jp,
        CompilationUnit,
        """
            interface Test {
                /**
                 * @param <
                 *   T> t hi
                 */
                <T> boolean test();
            }
        """.trimIndent()
    )

    @Test
    fun uses(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** @uses Test for something */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun unknownTags(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * See the {@unknown}.
             * @unknown uh oh
             */
            class Test {
            }
        """.trimIndent()
    )

    @Test
    fun otherBlockTags(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * @throws Exception
                 * @param s input
                 * @param <T> t type
                 */
                <T> T test(String t) throws Exception {
                    return null;
                }
            }
        """.trimIndent()
    )
}
