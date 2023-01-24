/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceRedundantFormatWithPrintfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceRedundantFormatWithPrintf());
    }

    @Test
    void doesNotModifyNonLiteralFormatString() {
        rewriteRun(
          java(
            """
              class Test<T> {
                  int test(String arg) {
                    String formatString = "hello %s%n";
                    System.out.print(String.format(formatString, arg));
                  }
              }
              """
          )
        );
    }

    @Test
    void modifiesCorrectArgumentGivenLocale() {
        rewriteRun(
          java(
            """
              import java.util.Locale;
              
              class Test<T> {
                  int test(String arg) {
                    System.out.println(String.format(Locale.ENGLISH, "hello %s", arg));
                  }
              }
              """,
            """
              import java.util.Locale;

              class Test<T> {
                  int test(String arg) {
                    System.out.printf(Locale.ENGLISH, "hello %s%n", arg);
                  }
              }
              """
          )
        );
    }

    @Test
    void transformsWhenTargetIsAnyPrintStream() {
        rewriteRun(
          java(
            """
              class PrintStreamSubclass extends java.io.PrintStream {}
              
              class Test<T> {
                  int test(PrintStreamSubclass stream, String arg) {
                    stream.println(String.format("hello %s", arg));
                  }
              }
              """,
            """
              class PrintStreamSubclass extends java.io.PrintStream {}

              class Test<T> {
                  int test(String arg) {
                    stream.printf("hello %s%n", arg);
                  }
              }
              """
          )
        );
    }

    @Test
    void transformsPrint() {
        rewriteRun(
          java(
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.print(String.format("hello %s", arg));
                  }
              }
              """,
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.printf("hello %s", arg);
                  }
              }
              """
          )
        );
    }

    @Test
    void transformsPrintWithTextBlockFormatString() {
        rewriteRun(
          java(
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.print(String.format(\"\"\"
                    hello %s\"\"\", arg));
                  }
              }
              """,
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.printf(\"\"\"
                    hello %s\"\"\", arg);
                  }
              }
              """
          )
        );
    }

    @Test
    void appendsNewlineForPrintln() {
        rewriteRun(
          java(
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.println(String.format("hello %s", arg));
                  }
              }
              """,
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.printf("hello %s%n", arg);
                  }
              }
              """
          )
        );
    }

    @Test
    void appendsNewlineForPrintlnWithTextBlockFormatString() {
        rewriteRun(
          java(
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.println(String.format(\"\"\"
                    hello %s\"\"\", arg));
                  }
              }
              """,
            """
              class Test<T> {
                  int test(String arg) {
                    System.out.printf(\"\"\"
                    hello %s%n\"\"\", arg);
                  }
              }
              """
          )
        );
    }
}
