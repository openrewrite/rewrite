/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddCommentToImportTest implements RewriteTest {
    private static final String SHORT_COMMENT = "Short comment to add";
    private static final String LONG_COMMENT = "This is a very long comment to add.\nThe comment uses multiple lines.";
    private static final String HEAVY_WRAP_COMMENT = "\nLine 1\nLine 2\nLine 3\n";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
          """
            package foo;
            public class Foo {}
            """,
          """
            package foo;
            public class Bar extends Foo {}
            """
        ));
    }

    @DocumentExample
    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(SHORT_COMMENT, "foo..*")),
          //language=java
          java(
            """
              package blah;
              // Existing Comment 1
              import foo.Foo;
              /* Existing Comment 2 */
              import foo.Bar;
              class Other {}
              """,
            """
              package blah;
              // Existing Comment 1
              /* Short comment to add */
              import foo.Foo;
              /* Existing Comment 2 */
              /* Short comment to add */
              import foo.Bar;
              class Other {}
              """
          )
        );
    }

    @Test
    void addLongComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(LONG_COMMENT, "foo..*")),
          //language=java
          java(
            """
              package blah;
              // Existing Comment 1
              import foo.Foo;
              /* Existing Comment 2 */
              import foo.Bar;
              class Other {}
              """,
            """
              package blah;
              // Existing Comment 1
              /*
               * This is a very long comment to add.
               * The comment uses multiple lines.
               */
              import foo.Foo;
              /* Existing Comment 2 */
              /*
               * This is a very long comment to add.
               * The comment uses multiple lines.
               */
              import foo.Bar;
              class Other {}
              """
          )
        );
    }

    @Test
    void addMultilineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(HEAVY_WRAP_COMMENT, "foo..*")),
          //language=java
          java(
            """
              package blah;
              // Existing Comment 1
              import foo.Foo;
              /* Existing Comment 2 */
              import foo.Bar;
              class Other {}
              """,
            """
              package blah;
              // Existing Comment 1
              /*
               * Line 1
               * Line 2
               * Line 3
               */
              import foo.Foo;
              /* Existing Comment 2 */
              /*
               * Line 1
               * Line 2
               * Line 3
               */
              import foo.Bar;
              class Other {}
              """
          )
        );
    }

    @Test
    void doNotAddCommentIfAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(SHORT_COMMENT, "foo..*")),
          //language=java
          java(
            """
              package blah;
              // Short comment to add
              import foo.Foo;
              /* Short comment to add */
              import foo.Bar;
              class Other {}
              """
          )
        );
    }
}
