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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import static org.openrewrite.java.Assertions.java;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class AddCommentToMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
    }

    @DocumentExample
    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethod(" Short comment to add", "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              package foo;
              public class Foo {
                  public void bar(String arg) {}
              }
              """,
            """
              package foo;
              public class Foo {
                  // Short comment to add
                  public void bar(String arg) {}
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void addLongComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethod(" This is a very long comment to add. The comment uses multiline comments, not single line.", "foo.Foo bar(..)", true)),
          //language=java
          java(
            """
              package foo;
              public class Foo {
                  public void bar(String arg) {}
              }
              """,
            """
              package foo;
              public class Foo {
                  /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
                  public void bar(String arg) {}
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void addSingleLineCommentToExistingSingleLineComments() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethod(" Short comment to add", "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              package foo;
              public class Foo {
                  // Existing single line comment
                  // Another existing single line comment
                  public void bar(String arg) {}
              }
              """,
            """
              package foo;
              public class Foo {
                  // Existing single line comment
                  // Another existing single line comment
                  // Short comment to add
                  public void bar(String arg) {}
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    @Disabled("This test fails due to \\ No newline at end of file https://github.com/openrewrite/rewrite/issues/4344")
    void addSingleLineCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethod(" Short comment to add", "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              package foo;
              public class Foo {
                  /**
                  * Existing multi line
                  * comment
                  */
                  public void bar(String arg) {}
              }
              """,
            """
              package foo;
              public class Foo {
                    /**
                    * Existing multi line
                    * comment
                    */
                    // Short comment to add
                  public void bar(String arg) {}
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    @Disabled("This test fails due to \\ No newline at end of file https://github.com/openrewrite/rewrite/issues/4344")
    void addLongCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethod(" This is a very long comment to add. The comment uses multiline comments, not single line.", "foo.Foo bar(..)", true)),
          //language=java
          java(
            """
              package foo;
              public class Foo {
                  /**
                  * Existing multi line
                  * comment
                  */
                  public void bar(String arg) {}
              }
              """,
            """
              package foo;
              public class Foo {
                    /**
                    * Existing multi line
                    * comment
                    */
                    /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
                  public void bar(String arg) {}
              }
              """
          )
        );
    }
}