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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class LiteralTest implements RewriteTest {

    @Test
    void intentionallyBadUnicodeCharacter() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      String[] strings = new String[] { "\\\\u{U1}", "\\\\u1234", "\\\\u{00AUF}" };
                  }
              }
              """
          )
        );
    }

    @Test
    void literalField() {
        rewriteRun(
          java(
            """
              class Test {
                  int n = 0;
              }
              """
          )
        );
    }

    @Test
    void literalCharacter() {
        rewriteRun(
          java(
            """
              class Test {
                  char c = 'a';
              }
              """
          )
        );
    }

    @Test
    void literalNumerics() {
        rewriteRun(
          java(
            """
              class Test {
                  double d1 = 1.0d;
                  double d2 = 1.0;
                  long l1 = 1L;
                  long l2 = 1;
              }
              """
          )
        );
    }

    @SuppressWarnings("OctalInteger")
    @Test
    void literalOctal() {
        rewriteRun(
          java(
            """
              class Test {
                  long l = 01L;
                  byte b = 01;
                  short s = 01;
                  int i = 01;
                  double d = 01;
                  float f = 01;
              }
              """
          )
        );
    }

    @Test
    void literalBinary() {
        rewriteRun(
          java(
            """
              class Test {
                  long l = 0b10L;
                  byte b = 0b10;
                  short s = 0b10;
                  int i = 0b10;
              }
              """
          )
        );
    }

    @Test
    void literalHex() {
        rewriteRun(
          java(
            """
              class Test {
                  long l = 0xA0L;
                  byte b = 0xA0;
                  short s = 0xA0;
                  int i = 0xA0;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/405")
    @Test
    void unmatchedSurrogatePair() {
        rewriteRun(
          java(
            """
              class Test {
                  char c1 = '\uD800';
                  char c2 = '\uDfFf';
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/405")
    @Test
    void unmatchedSurrogatePairInString() {
        rewriteRun(
          java(
            """
              class Test {
                  String s1 = "\uD800";
                  String s2 = "\uDfFf";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1387")
    @Test
    void multipleUnicodeEscapeCharactersAtValueSourceIndex() {
        rewriteRun(
          java(
            """
              class Test {
                  String s1 = "A\ud83c\udf09";
                  String s2 = "B\ud83c\udf09\ud83c\udf09";
                  String s3 = "C\uDfFf D \ud83c\udf09\ud83c\udf09";
              }
              """
          )
        );
    }

    @Test
    void transformString() {
        rewriteRun(
          java(
            """
              class Test {
                  String s = "foo ''";
              }
              """
          )
        );
    }

    @Test
    void nullLiteral() {
        rewriteRun(
          java(
            """
              class Test {
                  String s = null;
              }
              """
          )
        );
    }

    @Test
    void transformLong() {
        rewriteRun(
          java(
            """
              class Test {
                  Long l = 2L;
              }
              """
          )
        );
    }

    @SuppressWarnings("LongLiteralEndingWithLowercaseL")
    @Test
    void variationInSuffixCasing() {
        rewriteRun(
          java(
            """
              class Test {
                  Long l = 0l;
                  Long m = 0L;
              }
              """
          )
        );
    }

    @Test
    void escapedString() {
        rewriteRun(
          java(
            """
              class Test {
                  String s = "\\t	\\n";
              }
              """
          )
        );
    }

    @Test
    void escapedCharacter() {
        rewriteRun(
          java(
            """
              class Test {
                  char c = '\\'';
                  char tab = '	';
              }
              """
          )
        );
    }
}
