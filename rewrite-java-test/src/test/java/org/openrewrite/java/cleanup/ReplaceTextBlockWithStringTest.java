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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class ReplaceTextBlockWithStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceTextBlockWithString());
    }

    @Test
    void newLine() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "\\n";
              }
              """),
            14));
    }

    @Test
    void singleLine() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n";
              }
              """),
            14));
    }

    @Test
    void singleLineNoNewLineAtEnd() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1\"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1";
              }
              """),
            14));
    }

    @Test
    void multipleLines() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1
                          line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n" +
                          "line2\\n";
              }
              """),
            14));
    }

    @Test
    void multipleLinesNoNewLineAtEnd() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1
                          line2\"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n" +
                          "line2";
              }
              """),
            14));
    }

    @Test
    void indent() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1
                              line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n" +
                          "    line2\\n";
              }
              """),
            14));
    }

    @Test
    void startingEmptyLines() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"


                          line1
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "\\n" +
                          "\\n" +
                          "line1\\n";
              }
              """),
            14));
    }

    @Test
    void endingEmptyLines() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1


                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n" +
                          "\\n" +
                          "\\n";
              }
              """),
            14));
    }

    @Test
    void middleEmptyLines() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          line1


                          line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "line1\\n" +
                          "\\n" +
                          "\\n" +
                          "line2\\n";
              }
              """),
            14));
    }

    @Test
    void assignmentAndBlockSameLine() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str = \"\"\"
                          line1
                          line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str = "line1\\n" +
                          "line2\\n";
              }
              """),
            14));
    }

    @Test
    void singleLineComment() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          // Comment
                          \"\"\"
                          line1
                          line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          // Comment
                          "line1\\n" +
                          "line2\\n";
              }
              """),
            14));
    }

    @Test
    void multiLineComment() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          /* Comment
                           * Next line
                           */
                          \"\"\"
                          line1
                          line2
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          /* Comment
                           * Next line
                           */
                          "line1\\n" +
                          "line2\\n";
              }
              """),
            14));
    }

    @Test
    void doubleQuote() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          "line1"
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "\\"line1\\"\\n";
              }
              """),
            14));
    }

    @Test
    void threeDoubleQuotes() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          \\"\\"\\"line1\\"\\"\\"
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "\\"\\"\\"line1\\"\\"\\"\\n";
              }
              """),
            14));
    }

    @Test
    void unicode() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public class Test {
                  String str =
                          \"\"\"
                          Γειά σου Κόσμε
                          \"\"\";
              }
              """,
              """
              package com.example;

              public class Test {
                  String str =
                          "Γειά σου Κόσμε\\n";
              }
              """),
            14));
    }

}
