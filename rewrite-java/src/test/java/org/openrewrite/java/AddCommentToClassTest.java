/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

class AddCommentToClassTest implements RewriteTest {

    private static final String SHORT_COMMENT = " Short comment to add";
    private static final String LONG_COMMENT = " This is a very long comment to add. The comment uses multiline comments, not single line.";

    @DocumentExample
    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(SHORT_COMMENT, List.of("java.util.HashMap"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              // Short comment to add
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addLongComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(LONG_COMMENT, List.of("java.util.HashMap"), true)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addMultilineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass("\nLine 1\nLine 2\nLine 3\n", List.of("java.util.HashMap"), true)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              /*
              Line 1
              Line 2
              Line 3
              */
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addMultilineCommentOnSingleLine() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass("\nLine 1\nLine 2\nLine 3\n", List.of("java.util.HashMap"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              // Line 1 Line 2 Line 3\s
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addSingleLineCommentToExistingSingleLineComments() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(SHORT_COMMENT, List.of("java.util.HashMap"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              // Existing single line comment
              // Another existing single line comment
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              // Existing single line comment
              // Another existing single line comment
              // Short comment to add
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addSingleLineCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(SHORT_COMMENT, List.of("java.util.HashMap"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              /**
               * Existing multi line
               * comment
               */
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              /**
               * Existing multi line
               * comment
               */
              // Short comment to add
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addLongCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(LONG_COMMENT, List.of("java.util.HashMap"), true)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              /**
               * Existing multi line
               * comment
               */
              public class Example extends HashMap {
              }
              """,
            """
              import java.util.HashMap;
              
              /**
               * Existing multi line
               * comment
               */
              /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
              public class Example extends HashMap {
              }
              """
          )
        );
    }

    @Test
    void addSingleLineCommentToMultipleImpl() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(SHORT_COMMENT, List.of("java.util.HashMap", "java.lang.Runnable"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """,
            """
              import java.util.HashMap;
              
              // Short comment to add
              public class Example extends HashMap implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotAddSingleLineCommentToSingleImpl() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToClass(SHORT_COMMENT, List.of("java.util.HashMap", "java.lang.Runnable"), false)),
          //language=java
          java(
            """
              import java.util.HashMap;
              
              public class Example extends HashMap {
                  @Override
                  public void run() {
                  }
              }
              """
          )
        );
    }
}
