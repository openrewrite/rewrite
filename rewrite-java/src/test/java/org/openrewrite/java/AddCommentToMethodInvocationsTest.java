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
    private static final String SHORT_COMMENT = "Short comment to add";
    private static final String LONG_COMMENT = "This is a very long comment to add.\nThe comment uses multiple lines.";
    private static final String HEAVY_WRAP_COMMENT = "\nLine 1\nLine 2\nLine 3\n";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
          """
            package foo;
            public class Foo {
                public void bar(String arg) {}
                public boolean gar() {
                    return true;
                }
                public String har(boolean arg) {
                    return "";
                }
            }
            """
        ));
    }

    @DocumentExample
    @Test
    void addSingleLineComment() {
        rewriteRun(
          spec -> spec.recipes(
            new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo bar(..)"),
            new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo gar(..)")
          ),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Existing Comment
                      foo.bar("a");
                      boolean gar = /* Existing Comment */ foo.gar();
                      String har = foo.har(/* Existing Comment */foo.gar());
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Existing Comment
                      /* Short comment to add */
                      foo.bar("a");
                      boolean gar = /* Existing Comment */ /* Short comment to add */ foo.gar();
                      String har = foo.har(/* Existing Comment *//* Short comment to add */foo.gar());
                  }
              }
              """
          )
        );
    }

    @Test
    void addMultilineComment() {
        rewriteRun(
          spec -> spec.recipes(
            new AddCommentToMethodInvocations(LONG_COMMENT, "foo.Foo bar(..)"),
            new AddCommentToMethodInvocations(LONG_COMMENT, "foo.Foo gar(..)")
          ),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing Comment
                       */
                      foo.bar("a");
                      boolean gar = /* Existing Comment */ foo.gar();
                      String har = foo.har(/* Existing Comment */foo.gar());
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /*
                       * Existing Comment
                       */
                      /* This is a very long comment to add. * The comment uses multiple lines. */
                      foo.bar("a");
                      boolean gar = /* Existing Comment */ /* This is a very long comment to add. * The comment uses multiple lines. */ foo.gar();
                      String har = foo.har(/* Existing Comment *//* This is a very long comment to add. * The comment uses multiple lines. */foo.gar());
                  }
              }
              """
          )
        );
    }

    @Test
    void addAndSimplifyHeavilyWrappedComment() {
        rewriteRun(
          spec -> spec.recipes(
            new AddCommentToMethodInvocations(HEAVY_WRAP_COMMENT, "foo.Foo bar(..)"),
            new AddCommentToMethodInvocations(HEAVY_WRAP_COMMENT, "foo.Foo gar(..)")
          ),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      foo.bar("a");
                      boolean gar = foo.gar();
                      String har = foo.har(foo.gar());
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      /* Line 1 * Line 2 * Line 3 */
                      foo.bar("a");
                      boolean gar = /* Line 1 * Line 2 * Line 3 */ foo.gar();
                      String har = foo.har(/* Line 1 * Line 2 * Line 3 */foo.gar());
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotAddCommentIfAlreadyAdded() {
        rewriteRun(
          spec -> spec.recipes(
            new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo bar(..)"),
            new AddCommentToMethodInvocations(SHORT_COMMENT, "foo.Foo gar(..)")
          ),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method() {
                      Foo foo = new Foo();
                      // Short comment to add
                      foo.bar("a");
                      boolean gar = /* Short comment to add */ foo.gar();
                      String har = foo.har(/* Short comment to add */foo.gar());
                  }
              }
              """
          )
        );
    }

    @Test
    void escapeClosingTag() {
        rewriteRun(
          spec -> spec.recipe(new AddCommentToMethodInvocations("this is a */ terrible idea", "foo.Foo bar(..)")
          ),
          //language=java
          java(
            """
              import foo.Foo;

              class Other {
                  void method(Foo foo) {
                      foo.bar("a");
                  }
              }
              """,
            """
              import foo.Foo;

              class Other {
                  void method(Foo foo) {
                      /* this is a * terrible idea */
                      foo.bar("a");
                  }
              }
              """
          )
        );
    }
}
