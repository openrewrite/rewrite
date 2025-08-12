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
            """,
          """
            package foo.bar;
            public class Baz {
                public static void someStaticMethod() {}
            }
            """
        ));
    }

    @DocumentExample
    @Test
    void wildcardAndStaticImportsRecognized() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(SHORT_COMMENT, "foo.bar.Baz")),
          //language=java
          java(
            """
              package blah;
              // Will not match direct package
              import foo.*;
              import foo.bar.*;
              import foo.bar.Baz;
              import static foo.bar.Baz.someStaticMethod;
              class OtherOne {}
              """,
            """
              package blah;
              // Will not match direct package
              import foo.*;
              /* Short comment to add */
              import foo.bar.*;
              import foo.bar.Baz;
              import static foo.bar.Baz.someStaticMethod;
              class OtherOne {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              // Will not match direct package
              import foo.*;
              import foo.bar.Baz;
              import foo.bar.*;
              import static foo.bar.Baz.someStaticMethod;
              class OtherTwo {}
              """,
            """
              package blah;
              // Will not match direct package
              import foo.*;
              /* Short comment to add */
              import foo.bar.Baz;
              import foo.bar.*;
              import static foo.bar.Baz.someStaticMethod;
              class OtherTwo {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              // Will not match direct package
              import foo.*;
              import static foo.bar.Baz.someStaticMethod;
              import foo.bar.*;
              import foo.bar.Baz;
              class OtherThree {}
              """,
            """
              package blah;
              // Will not match direct package
              import foo.*;
              /* Short comment to add */
              import static foo.bar.Baz.someStaticMethod;
              import foo.bar.*;
              import foo.bar.Baz;
              class OtherThree {}
              """
          )
        );
    }

    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToImport(SHORT_COMMENT, "foo..*")),
          //language=java
          java(
            """
              package blah;
              // Existing Comment
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """,
            """
              package blah;
              // Existing Comment
              /* Short comment to add */
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              /* Existing Comment */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
              """,
            """
              package blah;
              /* Existing Comment */
              /* Short comment to add */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
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
              // Existing Comment
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """,
            """
              package blah;
              // Existing Comment
              /*
               * This is a very long comment to add.
               * The comment uses multiple lines.
               */
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              /* Existing Comment */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
              """,
            """
              package blah;
              /* Existing Comment */
              /*
               * This is a very long comment to add.
               * The comment uses multiple lines.
               */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
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
              // Existing Comment
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """,
            """
              package blah;
              // Existing Comment
              /*
               * Line 1
               * Line 2
               * Line 3
               */
              import foo.Foo;
              import foo.Bar;
              class OtherOne {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              /* Existing Comment */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
              """,
            """
              package blah;
              /* Existing Comment */
              /*
               * Line 1
               * Line 2
               * Line 3
               */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
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
              import foo.Bar;
              class OtherOne {}
              """
          ),
          //language=java
          java(
            """
              package blah;
              /* Short comment to add */
              import foo.Foo;
              import foo.Bar;
              class OtherTwo {}
              """
          )
        );
    }
}
