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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"JavadocDeclaration", "TrailingWhitespacesInTextBlock", "TextBlockMigration", "RedundantThrows", "ConcatenationWithEmptyString"})
class JavadocTest implements RewriteTest {

    @SuppressWarnings("JavadocReference")
    @Test
    void javadocs() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @Test
    void singleLineJavadocText() {
        rewriteRun(
          java(
            """
              /**   test */
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
    @Test
    void javaDocWithMultipleLeadingAsterisks() {
        rewriteRun(
          java(
            """
              /** ** * First '*' characters are treated as a margin until a non '*' is parsed.
               ** * @throws IOException validate cursor position.
               */
              class Test {
              }
               """
          )
        );
    }

    // All blank **********************************************************
    @Test
    void allBlank() {
        rewriteRun(
          java(
            """
              /**   */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void allBlankMultiline() {
        rewriteRun(
          java(
            """
              /**
               *
               *
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/965")
    @Test
    void emptyJavadoc() {
        rewriteRun(
          java(
            """
              class Test {
                  /***/
                  void empty() {}
                  /**
                   */
                  void onlyNewLine() {}
              }
              """
          )
        );
    }

    // Javadoc Annotations **********************************************

    // author
    @Test
    void author() {
        rewriteRun(
          java(
            """
              /**
               * @author name
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void authorPostFixedNumber() {
        rewriteRun(
          java(
            """
              /**
               * @author FirstName LastName 42
               *
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    // code
    @Test
    void code() {
        rewriteRun(
          java(
            """
              /** {@code int n = 1; } */
              class Test {
              }
              """
          )
        );
    }

    // deprecated
    @Test
    void deprecated() {
        rewriteRun(
          java(
            """
              /**
               * @deprecated reason
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    // docComment
    @Test
    void htmlComment() {
        rewriteRun(
          java(
            """
              /** <!-- comment --> */
              class Test {
              }
              """
          )
        );
    }

    // docRoot
    @Test
    void docRoot() {
        rewriteRun(
          java(
            """
              /**
               * See the <a href="{@docRoot}/copyright.html">Copyright</a>.
               */
              class Test {
              }
              """
          )
        );
    }

    // docType
    @Test
    void doctype() {
        rewriteRun(
          java(
            """
              /** <!doctype text > test */
              class Test {
              }
              """
          )
        );
    }

    // attributes (<a href>)
    @Test
    void multilineAttribute() {
        rewriteRun(
          java(
            """
              /**
               * <a href="
               * https://...html">
               * label</a>.
               */
              class Test {
              }
              """
          )
        );
    }

    // elements (<p></p> or <p/>).
    @Issue("https://github.com/openrewrite/rewrite/issues/1026")
    @Test
    void selfClosingHTMLElement() {
        rewriteRun(
          java(
            """
              /**
               *<p/>
               * text
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1047")
    @Test
    void preserveWhitespaceBeforeHTMLElement() {
        rewriteRun(
          java(
            """
              /**
               * <p>
               * <p/>
               * text <br>
               * text <br/>
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1089")
    @Test
    void whitespaceBeforeSelfClosingElement() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    // erroneous
    @Test
    void multipleLineErroneous() {
        rewriteRun(
          java(
            """
              /**
               * @see this
               * or that
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void erroneous() {
        rewriteRun(
          java(
            """
              /** {@version this is an erroneous tag } */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void unknownTags() {
        rewriteRun(
          java(
            """
              /**
               * See the {@unknown}.
               * @unknown uh oh
               */
              class Test {
              }
              """
          )
        );
    }

    // entity
    @Test
    void htmlEntity() {
        rewriteRun(
          java(
            """
              /** &amp; &amp ; &#12; */
              class Test {
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Test
    void exception() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              public class A {
                  /**
                   * @exception ex
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void hidden() {
        rewriteRun(
          java(
            """
              /**
               * @hidden value
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void indexOnly() {
        rewriteRun(
          java(
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
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/971")
    @Test
    void indexNoDescription() {
        rewriteRun(
          java(
            """
              /**
               * {@index term}
               * {@index term
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/971")
    @Test
    void indexTermAndDescription() {
        rewriteRun(
          java(
            """
              /**
               * {@index term description}
               * {@index term description
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void inheritDoc() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * {@inheritDoc}
                   * @return {@inheritDoc}
                   */
                  void test() {
                  }
              }
              """
          )
        );
    }

    // link
    // linkplain
    @Test
    void noMarginJavadocFirstLineTrailingWhitespace() {
        rewriteRun(
          java(
            """
              /**   
                 {@link int}
              */
              class Test {}
              """
          )
        );
    }


    @Test
    void leftMargin() {
        rewriteRun(
          java(
            """
              class Test {
                  /**   
                     {@link int}
                  */
                  String s;
              }
              """
          )
        );
    }

    @Test
    void noMarginJavadoc() {
        rewriteRun(
          java(
            """
              /**
                 {@link int}
              */
              class Test {}
              """
          )
        );
    }

    @Test
    void singleLineJavadoc() {
        rewriteRun(
          java(
            """
              /**   {@link int} */
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    void whitespaceBeforeAndAfterDelimiter() {
        rewriteRun(
          java(
            """
              import java.util.Map;
                            
              /**
               * {@link Map< String , Integer > }
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedLink() {
        rewriteRun(
          java(
            """
              /**
               * {@link java.util.List}
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedParameterizedTypeLink() {
        rewriteRun(
          java(
            """
              /**
               * {@link java.util.List<String>}
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedMethodLink() {
        rewriteRun(
          java(
            """
              /**
               * {@link java.util.List#add(Object) }
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void fieldLink() {
        rewriteRun(
          java(
            """
              import java.nio.charset.StandardCharsets;
                            
              /**
               *   {@link StandardCharsets#UTF_8 }
               *   {@linkplain StandardCharsets#UTF_8 }
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void thisFieldLink() {
        rewriteRun(
          java(
            """
              /**
               *   {@link #n }
               */
              class Test {
                  int n;
              }
              """
          )
        );
    }

    @Test
    void thisMethodLink() {
        rewriteRun(
          java(
            """
              /**
               *   {@link #test() }
               */
              class Test {
                  void test() {}
              }
              """
          )
        );
    }

    @Test
    void primitiveLink() {
        rewriteRun(
          java(
            """
              /**
               * Line 1
               * Line 2
               * {@link int}
               * @param <T> t
               */
              class Test<T> {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    void multiParameterizedType() {
        rewriteRun(
          java(
            """
              import java.util.Map;
                            
              /**
               * {@link Map<String, Map<String, Integer>>} multiple parameterized type
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1274")
    @Test
    void parameterizedType() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Test
    void multipleReferenceParameters() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              class Test {
                  /**
                   * {@link ListenerUtils#getExceptionFromHeader(ConsumerRecord, String, LogAccessor)}
                   */
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/963")
    @Test
    void blankLink() {
        rewriteRun(
          java(
            """
              /**
               * {@link}
               */
              class Test {
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Issue("https://github.com/openrewrite/rewrite/issues/968")
    @Test
    void missingBracket() {
        rewriteRun(
          java(
            """
              /**
               * {@link missing.bracket
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void methodReferenceNoParameters() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * {@linkplain Thread#interrupt}
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Issue("https://github.com/openrewrite/rewrite/issues/967")
    @Test
    void multilineLink() {
        rewriteRun(
          java(
            """
              /**
               * {@link
               * multiline}.
               */
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/964")
    @Test
    void constructorLink() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * {@link java.util.List()}
                   */
                  void test() {
                  }
              }
              """
          )
        );
    }

    // literal
    @Test
    void literal() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @literal
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/942")
    @Test
    void nullLiteral() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * {@literal null}.
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @Test
    void emptyParam() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @param
                   */
                  void method(int val) {}
              }
              """
          )
        );
    }

    @Test
    void param() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @param val
                   */
                  void method(int val) {}
              }
              """
          )
        );
    }

    @Test
    void singleLineParam() {
        rewriteRun(
          java(
            """
              /**   @param <T> t */
              class Test<T> {}
              """
          )
        );
    }

    @Test
    void lineBreakInParam() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1412")
    @Test
    void paramWithMultilineHtmlAttributeNewLineBeforeEquals() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * @param contentType <a href
                   *    = "https://www..."> label</a>
                   */
                  boolean test(int contentType);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/941")
    @Test
    void paramWithMultilineHtmlAttribute() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * @param contentType <a href=
                   *                    "https://www...">
                   *                    label</a>
                   */
                  boolean test(int contentType);
              }
              """
          )
        );
    }

    @Test
    void descriptionOnNewLine() {
        rewriteRun(
          java(
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
        );
    }

    // provides
    @Test
    void provide() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @provides int
                   */
                  void method(int val) {}
              }
              """
          )
        );
    }

    // return
    @Test
    void returnTag() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @return id
                   */
                  int method(int val) {}
              }
              """
          )
        );
    }

    @Test
    void returnWithoutMargin() {
        rewriteRun(
          java(
            """
              public class Test {
                  /** Text
                   @return No margin
                   */
                  public int test() {
                      return 0;
                  }
              }
              """
          )
        );
    }

    // see
    @Test
    void see() {
        rewriteRun(
          java(
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
        );
    }

    @Test
    void methodFound() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * @see java.io.ByteArrayOutputStream#toString(String)
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Issue("https://github.com/openrewrite/rewrite/issues/945")
    @Test
    void methodNotFound() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()),
          java(
            """
              interface Test {
                  /**
                   * @see Math#cosine(double)
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Issue("https://github.com/openrewrite/rewrite/issues/944")
    @Test
    void typeNotFound() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              interface Test {
                  /**
                   * {@link SymbolThatCannotBeFound}
                   * @see Mathy#cos(double)
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/941")
    @Test
    void seeWithMultilineAttribute() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * @see <a href="https://www...">
                   *            label</a>
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @Test
    void serial() {
        rewriteRun(
          java(
            """
              /**
               * @serial
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void serialData() {
        rewriteRun(
          java(
            """
              /**
               * @serialData
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void since() {
        rewriteRun(
          java(
            """
              /** @since 1.0 */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void summary() {
        rewriteRun(
          java(
            """
              /** {@summary test description } */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void uses() {
        rewriteRun(
          java(
            """
              /** @uses Test for something */
              class Test {
                  /** @uses Test for something */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void throwsException() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * @throws Exception
                   */
                  <T> T test(String t) throws Exception {
                      return null;
                  }
              }
              """
          )
        );
    }

    // value
    @Test
    void value() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    // version
    @Test
    void version() {
        rewriteRun(
          java(
            """
              /**
               * @version 1.0.0
               */
              public class A {
                  void method() {}
              }
              """
          )
        );
    }

    // Whitespace
    @Test
    void starMarginWithFirstLineLeadingSpace() {
        rewriteRun(
          java(
            """
              /**   
                *       Line 1
                */
              class Test<T> {}
              """
          )
        );
    }

    @SuppressWarnings("JavadocBlankLines")
    @Issue("https://github.com/openrewrite/rewrite/issues/2139")
    @Test
    void javaDocEndsOnSameLine() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * Javadoc
                   *
                   * **/
                  void method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1409")
    @Test
    void trailingWhitespaceWithWhitespaceOnEmptyLine() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1094")
    @Test
    void trailingWhitespaceAfterText() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * Text with trailing whitespace.    
                   * More trailing whitespace    
                   */
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1094")
    @Test
    void trailingWhitespaceAfterAnnotation() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * @param arg test text.    
                   * More trailing whitespace    
                   */
                  void method(String arg) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/957")
    @Test
    void commentMissingMultipleAsterisks() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1397")
    @Test
    void textWithBlankNewLines() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                  * JavaDocs treats whitespace differently when new lines exist
                  
                  
                  * with whitespace that is contained in pure text.
                  */
                  void method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1374")
    @Test
    void tagAfterBlankNewLines() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * New lines with whitespace followed by a param.
                   
                   
                   * @return void
                   */
                  void method() {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Test
    void consecutiveLineBreaksWithNoMargin() {
        rewriteRun(
          java(
            """
              class Test {
                  /** 
                   * @param oboFile the file to be parsed
                            
                   * @return the ontology represented as a BioJava ontology file
                   */
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void whitespaceBeforeNonLeadingText() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * @return <code>true</code>
                   * <code>false</code> non-leading text.
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocReference")
    @Test
    void multipleLinesBeforeTag() {
        rewriteRun(
          java(
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
        );
    }

    @Test
    void whitespaceOnBlankLineBetweenBodyAndTags() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * Returns something.
                   * 
                   * @return true
                   */
                  boolean test();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2046")
    @Test
    void trailingWhitespaceAndMultilineMargin() {
        rewriteRun(
          java(
            """
              interface Test {
                  /**
                   * Text followed by whitespace, and multiple new lines with/without whitespace.        
                   *
                   * 
                   */
                  void method();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1078")
    @Test
    void seeWithMultilineMethodInvocation() {
        rewriteRun(
          java(
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void javaDocWithCRLF() {
        rewriteRun(
          java("" +
                          "/**\r\n" +
                          " * JavaDoc.\r\n" +
                          " */\r\n" +
                          "public class A {\r\n" +
                          "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1411")
    @Test
    void noMarginWithCRLF() {
        rewriteRun(
          java("" +
                          "/**\r\n" +
                          " * Line 1.\r\n" +
                          "   Text with no margin.\r\n" +
                          " */\r\n" +
                          "public class A {\r\n" +
                          "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1411")
    @Test
    void emptyLinesWithCRLF() {
        rewriteRun(
          java("" +
                          "public class A {\r\n" +
                          "  /** Text \r\n" +
                          "         \r\n" +
                          "         \r\n" +
                          "     @param arg0 desc\r\n" +
                          "   */\r\n" +
                          "  void method(int arg0) {}\r\n" +
                          "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/976")
    @Test
    void multilineJavaDocWithCRLF() {
        rewriteRun(
          java("" +
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
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Issue("https://github.com/openrewrite/rewrite/issues/976")
    @Test
    void multilineWithThrowsAndCRLF() {
        rewriteRun(
          java("" +
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
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1090")
    @Test
    void paramNoDescriptionWithCRLF() {
        rewriteRun(
          java("" +
                          "import org.foo;\r\n" +
                          "\r\n" +
                          "public class A {\r\n" +
                          "    /**\r\n" +
                          "     * @param arg0\r\n" +
                          "     */\r\n" +
                          "    void method(String arg0) {}\r\n" +
                          "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1494")
    @Test
    void trailingWhitespaceWithLF() {
        rewriteRun(
          java("" +
                          "/**\n" +
                          " * Text followed by trailing whitespace with CRLF.\n" +
                          " * \n" +
                          " */\n" +
                          "class A {}"
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3530")
    void arrayTypeLiterals() {
        rewriteRun(
          java("" +
            "/**\n" +
            "  * Create an instance of {@link byte[]} and {@link byte[][]}\n" +
            "  */\n" +
            "class A {}"
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3530")
    @MinimumJava11
    void arrayTypeLiterals2() {
        rewriteRun(
          java("" +
            "/**\n" +
            " * <p>Values are converted to strings using {@link java.util.Arrays#compare(Comparable[], Comparable[])}}.\n" +
            " */\n" +
            "class A {}"
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3575")
    void varargsMethod() {
        rewriteRun(
          java(
            """
              class A {
                  /**
                   * A dummy main method. This method is not actually called, but we'll use its Javadoc comment to test that
                   * OpenRewrite can handle references like the following: {@link A#varargsMethod(String...)}.
                   *
                   * @param args The arguments to the method.
                   */
                  public static void main(String[] args) {
                      System.out.println("Hello, world! This is my original class' main method.");
                  }
                  public static void varargsMethod(String... args) {
                      System.out.println("Hello, world! This is my original class' varargs method.");
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3575")
    void varargsWithPrefix() {
        rewriteRun(
          // for some reason the compiler AST's type attribution is incomplete here
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              class A {
                  /**
                   * A dummy main method. This method is not actually called, but we'll use its Javadoc comment to test that
                   * OpenRewrite can handle references like the following: {@link A#varargsMethod( Object, String...)} }.
                   *
                   * @param args The arguments to the method.
                   */
                  public static void main(String[] args) {
                      System.out.println("Hello, world! This is my original class' main method.");
                  }
                  public static void varargsMethod(Object o, String... args) {
                      System.out.println("Hello, world! This is my original class' varargs method.");
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3575")
    void arrayMethod() {
        rewriteRun(
          java(
            """
              class A {
                  /**
                   * A dummy main method. This method is not actually called, but we'll use its Javadoc comment to test that
                   * OpenRewrite can handle references like the following: {@link A#main(String[])}.
                   *
                   * @param args The arguments to the method.
                   */
                  public static void main(String[] args) {
                      System.out.println("Hello, world! This is my original class' main method.");
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyAttributes() {
        rewriteRun(
          java(
            """
              /**
               * DEFINE TENANCY TenantB AS <TenantB OCID>
               * ENDORSE GROUP <TenantA user group name> TO {OBJECTSTORAGE_NAMESPACE_READ} IN TENANCY TenantB
               *
               * DEFINE TENANCY TenantA AS <TenantA OCID>
               * DEFINE GROUP TenantAGroup AS <TenantA user group OCID>
               * ADMIT GROUP TenantAGroup OF TENANCY TenantA TO {OBJECTSTORAGE_NAMESPACE_READ} IN TENANCY
               */
              class Test {}
              """
          )
        );
    }

    @Test
    void trailingTab() {
        rewriteRun(
          java(
            """
              /**
               * See <a href="">here</a>\t
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    void returnOpeningAndClosingBrace() {
        rewriteRun(
          java(
            """
              interface Test {
              	/**
              	 * {@return 42}
              	 */
              	int foo();
              }
              """
          )
        );
    }

    @Test
    void returnOpeningBraceOnly() {
        rewriteRun(
          java(
            """
              interface Test {
              	/**
              	 * {@return 42
              	 */
              	int foo();
              }
              """
          )
        );
    }

    @Disabled
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3650")
    void unicodeEscape() {
        rewriteRun(
          java(
            """
              interface Test {
              	/**
              	 * Return the {@code \\u0000} codepoint.
              	 */
              	int foo();
              }
              """
          )
        );
    }
}
