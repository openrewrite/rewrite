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
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface CommentTest : JavaTreeTest {

    @Test
    fun backToBackMultilineComments(jp: JavaParser)  = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            class Test {
                /*
                    Comment 1
                *//*
                    Comment 2
                */
            }
        """
    )

    @Test
    fun multilineNestedInsideSingleLine(jp: JavaParser) = assertParsePrintAndProcess(
            jp,
            CompilationUnit,
            """
                class Test {// /*
                }
            """
    )

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
    fun singleLineParam(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /**   @param <T> t */
            class Test<T> {}
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
    fun index(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** {@index test description of term } */
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
    fun version(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        CompilationUnit,
        """
            /** @version 1.0 */
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
}
