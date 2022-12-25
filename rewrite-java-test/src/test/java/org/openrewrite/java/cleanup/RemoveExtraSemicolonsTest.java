/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("UnnecessarySemicolon")
class RemoveExtraSemicolonsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExtraSemicolons());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1587")
    @Test
    void enumSemicolons() {
        rewriteRun(
          java(
            """
              public enum FRUITS {
                  BANANA,
                  APPLE;
              }
              """,
            """
              public enum FRUITS {
                  BANANA,
                  APPLE
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1587")
    @Test
    void enumSemicolonsWithOtherStatements() {
        rewriteRun(
          java(
            """
              public enum FRUITS {
                  BANANA,
                  APPLE;
                  
                  void hiFruit() {}
              }
              """
          )
        );
    }

    @Test
    void emptyBlockStatements() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      ;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void test() {
                      try (ByteArrayInputStream b = new ByteArrayInputStream(new byte[10]);
                            Reader r = new InputStreamReader(b);) {
                      }
                  }
              }
              """,
            """
              import java.io.*;
              class Test {
                  void test() {
                      try (ByteArrayInputStream b = new ByteArrayInputStream(new byte[10]);
                            Reader r = new InputStreamReader(b)) {
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.Try visitTry(J.Try t, Object o) {
                    List<J.Try.Resource> resources = t.getResources();
                    assertThat(resources).isNotNull();
                    assertThat(resources.get(0).isTerminatedWithSemicolon()).isTrue();
                    assertThat(resources.get(1).isTerminatedWithSemicolon()).isFalse();
                    return t;
                }
            }.visit(cu, 0))
          )
        );
    }
}
