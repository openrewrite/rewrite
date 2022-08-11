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

@Suppress("JavadocDeclaration")
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
    @Issue("https://github.com/openrewrite/rewrite/issues/2139")
    @Disabled
    fun javadocEndingOnSameLine(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            package an.example.error;
                        
            import java.util.ArrayList;
            import java.util.List;
            
            public class Test {
            
                /**
                 * A doc
                 *
                 * **/
                public void aMethod() throws Exception, RuntimeException{
                }
            }
        """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
    @Test
    fun javaDocWithMultipleLeadingAsterisks(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
           /** ** * First '*' characters are treated as a margin until a non '*' is parsed.
            ** * @throws IOException validate cursor position.
            */
           class Test {
           }
        """.trimIndent()
    )

    // All blank **********************************************************
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

    @Issue("https://github.com/openrewrite/rewrite/issues/965")
    @Test
    fun emptyJavadoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /***/
                void empty() {}
                /**
                 */
                void onlyNewLine() {}
            }
        """.trimIndent()
    )

    // Javadoc Annotations **********************************************

    // author
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
        """.trimIndent()
    )

    @Test
    fun authorPostFixedNumber(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * @author FirstName LastName 42
             *
             */
            public class A {
                void method() {}
            }
        """.trimIndent()
    )

    // code
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

    // deprecated
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
        """.trimIndent()
    )

    // docComment
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

    // docRoot
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

    // docType
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

    // attributes (<a href>)
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

    // elements (<p></p> or <p/>).
    @Issue("https://github.com/openrewrite/rewrite/issues/1026")
    @Test
    fun selfClosingHTMLElement(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             *<p/>
             * text
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1047")
    @Test
    fun preserveWhitespaceBeforeHTMLElement(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * <p>
             * <p/>
             * text <br>
             * text <br/>
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1089")
    @Test
    fun whitespaceBeforeSelfClosingElement(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            package org.foo;
            
            /**
             * Type of an Opening Time.
             * <ul>
             * <li>DELIVERY (text a)</li>
             * <li>PICKUP (text b)</li> <br />
             * </ul>
             */
            public enum OpenTimeType {
                DELIVERY,
                PICKUP
            }
        """.trimIndent()
    )


    // erroneous
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

    // entity
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

    // exception
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
        """.trimIndent()
    )

    // hidden
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
        """.trimIndent()
    )

    // index
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
        """.trimIndent()
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

    // inheritDoc
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

    // link
    // linkplain
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
    fun singleLineJavadoc(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   {@link int} */
            class Test {}
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    fun whitespaceBeforeAndAfterDelimiter(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.util.Map;
            
            /**
             * {@link Map< String , Integer > }
             */
            class Test {
            }
        """.trimIndent()
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
        """.trimIndent()
    )

    @Test
    fun fullyQualifiedParameterizedTypeLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**
             * {@link java.util.List<String>}
             */
            public class A {
                void method() {}
            }
        """.trimIndent()
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    fun multiParameterizedType(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.util.Map;
            
            /**
             * {@link Map<String, Map<String, Integer>>} multiple parameterized type
             */
            class Test {
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    fun parameterizedType(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.util.List;
            
            class Test {
                /**
                 * @return - {@link List<String>} - description.
                 * @throws Exception - exception.
                 */
                List<String> method() throws Exception {
                }
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

    @Issue("https://github.com/openrewrite/rewrite/issues/964")
    @Test
    fun constructorLink(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * {@link java.util.List()}
                 */
                void test() {
                }
            }
        """.trimIndent()
    )

    // literal
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

    // param
    @Test
    fun emptyParam(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            public class A {
                /**
                 * @param
                 */
                void method(int val) {}
            }
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
    fun lineBreakInParam(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            interface Test {
                /**
                 * @param <
                 *   T> t hi
                 * @param 
                 *   val
                 */
                <T> boolean test(int val);
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1412")
    @Test
    fun paramWithMultilineHtmlAttributeNewLineBeforeEquals(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            interface Test {
                /**
                 * @param contentType <a href
                 *    = "https://www..."> label</a>
                 */
                boolean test(int contentType);
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
        """.trimIndent()
    )

    // provides
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
        """.trimIndent()
    )

    // return
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
        """.trimIndent()
    )

    @Test
    fun returnWithoutMargin(jp: JavaParser) = assertParsePrintAndProcess(
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

    // see
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

    // serial
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
        """.trimIndent()
    )

    // serialData
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
        """.trimIndent()
    )

    // since
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

    // summary
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

    // uses
    @Test
    fun uses(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** @uses Test for something */
            class Test {
                /** @uses Test for something */
                void method() {}
            }
        """.trimIndent()
    )

    // throws
    @Test
    fun throws(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * @throws Exception
                 */
                <T> T test(String t) throws Exception {
                    return null;
                }
            }
        """.trimIndent()
    )

    // value
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

    // version
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
        """.trimIndent()
    )

    // Whitespace
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1409")
    @Test
    fun trailingWhitespaceWithWhitespaceOnEmptyLine(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * Text with trailing whitespace.    
                 * 
                 * @param arg desc
                 */
                void method(String arg) {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1094")
    @Test
    fun trailingWhitespaceAfterText(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * Text with trailing whitespace.    
                 * More trailing whitespace    
                 */
                void method() {
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1094")
    @Test
    fun trailingWhitespaceAfterAnnotation(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * @param arg test text.    
                 * More trailing whitespace    
                 */
                void method(String arg) {
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1397")
    @Test
    fun textWithBlankNewLines(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                * JavaDocs treats whitespace differently when new lines exist
                
                
                * with whitespace that is contained in pure text.
                */
                void method() {}
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1374")
    @Test
    fun tagAfterBlankNewLines(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            class Test {
                /**
                 * New lines with whitespace followed by a param.
                 
                 
                 * @return void
                 */
                void method() {
                }
            }
        """.trimIndent()
    )

    @Test
    fun consecutiveLineBreaksWithNoMargin(jp: JavaParser) = assertParsePrintAndProcess(
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2046")
    @Test
    fun trailingWhitespaceAndMultilineMargin(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            interface Test {
                /**
                 * Text followed by whitespace, and multiple new lines with/without whitespace.        
                 *
                 * 
                 */
                void method();
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1078")
    @Test
    fun seeWithMultilineMethodInvocation(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            import java.lang.Math;
            
            interface Test {
                /**
                 * @see Math#pow(
                 * 
                 *    double   
                 * 
                 *
                 *    ,    
                 * double
                 * 
                 * )
                 */
                boolean test();
            }
        """.trimIndent()
    )

    // CRLF
    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun javaDocWithCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "/**\r\n" +
                " * JavaDoc.\r\n" +
                " */\r\n" +
                "public class A {\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1411")
    @Test
    fun noMarginWithCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "/**\r\n" +
                " * Line 1.\r\n" +
                "   Text with no margin.\r\n" +
                " */\r\n" +
                "public class A {\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1411")
    @Test
    fun emptyLinesWithCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "public class A {\r\n" +
                "  /** Text \r\n" +
                "         \r\n" +
                "         \r\n" +
                "     @param arg0 desc\r\n" +
                "   */\r\n" +
                "  void method(int arg0) {}\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/976")
    @Test
    fun multilineJavaDocWithCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "/**\r\n" +
                " * Line 1.\r\n" +
                " * Line 2.\r\n" +
                " */\r\n" +
                "public class A {\r\n" +
                "    /**\r\n" +
                "     * Line 1.\r\n" +
                "     * Line 2.\r\n" +
                "     */\r\n" +
                "    void method() {}\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/976")
    @Test
    fun multilineWithThrowsAndCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "import java.io.IOException;\r\n" +
                "\r\n" +
                "public class A {\r\n" +
                "    /**\r\n" +
                "     * Line 1.\r\n" +
                "     * Line 2.\r\n" +
                "     * @throws IOException text.\r\n" +
                "     */\r\n" +
                "    void method() throws IOException {}\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1090")
    @Test
    fun paramNoDescriptionWithCRLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "import org.foo;\r\n" +
                "\r\n" +
                "public class A {\r\n" +
                "    /**\r\n" +
                "     * @param arg0\r\n" +
                "     */\r\n" +
                "    void method(String arg0) {}\r\n" +
                "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1494")
    @Test
    fun trailingWhitespaceWithLF(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit, "" +
                "/**\n" +
                " * Text followed by trailing whitespace with CRLF.\n" +
                " * \n" +
                " */\n" +
                "class A {}"
    )
}
