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

class AddCommentToMethodInvocationsTest implements RewriteTest {
    private static final String SHORT_COMMENT = " Short comment to add";
    private static final String LONG_COMMENT = " This is a very long comment to add. The comment uses multiline comments, not single line.";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
          """
            package foo;
            public class Foo {
                public void bar(String arg) {}
            }
            """
        ));
    }

    @DocumentExample
    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              import foo.Foo;
   
              class Other {
                  void method() {
                      Foo foo = new Foo();
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Short comment to add
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addLongComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations(LONG_COMMENT, "foo.Foo bar(..)", true)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addMultilineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations("\nLine 1\nLine 2\nLine 3\n", "foo.Foo bar(..)", true)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                      Line 1
                      Line 2
                      Line 3
                      */
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addMultilineCommentOnSingleLine() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations("\nLine 1\nLine 2\nLine 3\n", "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Line 1 Line 2 Line 3\s
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addSingleLineCommentToExistingSingleLineComments() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Existing single line comment
                      // Another existing single line comment
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Existing single line comment
                      // Another existing single line comment
                      // Short comment to add
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addSingleLineCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo bar(..)", false)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing multi line
                       * comment
                       */
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing multi line
                       * comment
                       */
                      // Short comment to add
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }

    @Test
    void addLongCommentToExistingMultiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations(LONG_COMMENT, "foo.Foo bar(..)", true)),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing multi line
                       * comment
                       */
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing multi line
                       * comment
                       */
                      /* This is a very long comment to add. The comment uses multiline comments, not single line.*/
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }
}
