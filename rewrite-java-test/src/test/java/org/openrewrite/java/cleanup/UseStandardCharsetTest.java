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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("CharsetObjectCanBeUsed")
class UseStandardCharsetTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseStandardCharset());
    }

    @Test
    void notAStandardCharset() {
        rewriteRun(
          java(
            """
              import java.nio.charset.Charset;

              class Test {
                  Charset WINDOWS_1252 = Charset.forName("Windows-1252");
              }
              """
          )
        );
    }

    @Test
    void changeCharsetForName() {
        rewriteRun(
          java(
            """
              import java.nio.charset.Charset;

              class Test {
                  Charset US_ASCII = Charset.forName("US-ASCII");
                  Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
                  Charset UTF_8 = Charset.forName("UTF-8");
                  Charset UTF_16 = Charset.forName("UTF-16");
                  Charset UTF_16BE = Charset.forName("UTF-16BE");
                  Charset UTF_16LE = Charset.forName("UTF-16LE");
              }
              """,
            """
              import java.nio.charset.Charset;
              import java.nio.charset.StandardCharsets;

              class Test {
                  Charset US_ASCII = StandardCharsets.US_ASCII;
                  Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
                  Charset UTF_8 = StandardCharsets.UTF_8;
                  Charset UTF_16 = StandardCharsets.UTF_16;
                  Charset UTF_16BE = StandardCharsets.UTF_16BE;
                  Charset UTF_16LE = StandardCharsets.UTF_16LE;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2450")
    @Test
    void convertAnyValidName() {
        rewriteRun(
          java(
            """
              import java.nio.charset.Charset;

              class Test {
                  Charset UTF_8_A = Charset.forName("utf-8");
                  Charset UTF_8_B = Charset.forName("utf8");
                  Charset UTF_8_C = Charset.forName("UTF8");
              }
              """,
            """
              import java.nio.charset.Charset;
              import java.nio.charset.StandardCharsets;

              class Test {
                  Charset UTF_8_A = StandardCharsets.UTF_8;
                  Charset UTF_8_B = StandardCharsets.UTF_8;
                  Charset UTF_8_C = StandardCharsets.UTF_8;
              }
              """
          )
        );
    }
}
