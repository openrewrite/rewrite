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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

@SuppressWarnings("UnnecessaryStringEscape")
class ReplaceTextBlockWithStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceTextBlockWithString()).allSources(s -> s.markers(javaVersion(14)));
    }

    @DocumentExample
    @Test
    void newLine() {
        rewriteRun(
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
            """));
    }

    @Test
    void singleLine() {
        rewriteRun(
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
            """));
    }

    @Test
    void singleLineNoNewLineAtEnd() {
        rewriteRun(
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
            """));
    }

    @Test
    void multipleLines() {
        rewriteRun(
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
            """));
    }

    @Test
    void multipleLinesNoNewLineAtEnd() {
        rewriteRun(
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
            """));
    }

    @Test
    void indent() {
        rewriteRun(
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
            """));
    }

    @Test
    void startingEmptyLines() {
        rewriteRun(
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
            """));
    }

    @Test
    void endingEmptyLines() {
        rewriteRun(
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
            """));
    }

    @Test
    void middleEmptyLines() {
        rewriteRun(
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
            """));
    }

    @Test
    void assignmentAndBlockSameLine() {
        rewriteRun(
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
            """));
    }

    @Test
    void singleLineComment() {
        rewriteRun(
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
            """));
    }

    @Test
    void multiLineComment() {
        rewriteRun(
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
            """));
    }

    @Test
    void doubleQuote() {
        rewriteRun(
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
            """));
    }

    @Test
    void threeDoubleQuotes() {
        rewriteRun(
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
            """));
    }

    @Test
    void unicode() {
        rewriteRun(
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
            """));
    }

}
