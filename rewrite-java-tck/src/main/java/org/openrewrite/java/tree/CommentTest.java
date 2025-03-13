/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CommentTest implements RewriteTest {

    @Test
    void backToBackMultilineComments() {
        rewriteRun(
          java(
            """
              class Test {
                  /*
                      Comment 1
                  *//*
                      Comment 2
                  */
              }
              """
          )
        );
    }

    @Test
    void multilineNestedInsideSingleLine() {
        rewriteRun(
          java(
            """
              class Test {// /*
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4995")
    @Test
    void trailingComment() {
        rewriteRun(
          java(
            """
              abstract class Test {
                void alert(String msg) /*-{ $wnd.alert(msg); }-*/;
              }
              """
          )
        );
    }

    @Test
    void commentAsLastLine() {
        rewriteRun(
          java(
            """
            class A {
            }
            //
            """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/5090")
    void multiLineCommentWithUrl() {
        rewriteRun(
          java(
            """
              package hello;
              public class Test {
                public static void test() {
                  /*addItem("Site A", "https://hello.com/A");*/
                }
              }
              """
          )
        );
    }
}
