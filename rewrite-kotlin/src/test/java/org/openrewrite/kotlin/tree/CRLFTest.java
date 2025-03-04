/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("All")
class CRLFTest implements RewriteTest {

    @Test
    void crlf() {
        rewriteRun(
          kotlin(
            "package some.other.name\r\n" +
              "class A { }\r\n" +
              "class B { }"
          )
        );
    }

    @Test
    void consecutiveCRLF() {
        rewriteRun(
          kotlin(
            "package some.other.name\r\n\r\n\r\n" +
              "class A { }\r\n\r\n\r\n" +
              "class B { }"
          )
        );
    }

    @Test
    void crlfAfterComment() {
        rewriteRun(
          kotlin(
            "class Test \r\n {//some comment\r\n}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/520")
    @Test
    void crlfInKdoc() {
        String windowsJavadoc =
          "/**\r\n" +
          " *\r\n" +
          " * Foo\r\n" +
          " */\r\n" +
          "class Test {\r\n" +
          "}";

        rewriteRun(
          kotlin(
            windowsJavadoc
          )
        );
    }

    @Test
    void crlfInBlockComment() {
        rewriteRun(
          kotlin(
            "public class A {\r\n" +
              "/*a\r\n" +
              "  b*/\r\n" +
              "public fun method() {}\r\n" +
              "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/402")
    @Test
    void crlfInMultilineString() {
        rewriteRun(
          kotlin(
            "val s = \"\"\"\r\n" +
              "l1\r\n" +
              "l2\n" +
              "\"\"\""
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/438")
    @Test
    void crlfInMultilineStringWithParameters() {
        rewriteRun(
          kotlin(
            "val a = \"\"\r\n" +
            "val s = \"\"\"${a}\r\n" +
            "l1\r\n" +
            "l2\n" +
            "\"\"\""
          )
        );
    }
}
