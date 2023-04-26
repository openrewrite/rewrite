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
import org.openrewrite.Issue;
import org.openrewrite.internal.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceStringBuilderWithStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceStringBuilderWithString());
    }

    @DocumentExample
    @Test
    void replaceLiteralConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void foo() {
                      String s = new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      String s = "A" + "B";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceLiteralConcatenationWithReturn() {
        rewriteRun(
          java(
            """
              class A {
                  String foo() {
                      return new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class A {
                  String foo() {
                      return "A" + "B";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceCombinedConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  String bar() {
                      String str1 = "Hello";
                      String str2 = name();
                      return new StringBuilder().append(str1).append(str2).append(getSuffix()).toString();
                  }
                  String name() {
                      return "world";
                  }
                  String getSuffix() {
                      return "!";
                  }
              }
              """,
            """
              class A {
                  String bar() {
                      String str1 = "Hello";
                      String str2 = name();
                      return str1 + str2 + getSuffix();
                  }
                  String name() {
                      return "world";
                  }
                  String getSuffix() {
                      return "!";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInChainedMethods() {
        rewriteRun(
          java(
            """
              class A {
                  void foo() {
                      int len = new StringBuilder().append("A").append("B").toString().length();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      int len = ("A" + "B").length();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2930")
    @Test
    void withConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  void method() {
                      String key1 = new StringBuilder(10).append("_").append("a").toString();
                      String key2 = new StringBuilder(name()).append("_").append("a").toString();
                      String key3 = new StringBuilder("m").append("_").append("a").toString();
                      String key4 = new StringBuilder("A" + "B").append("C").toString();
                  }
                  String name() {
                      return "name";
                  }
              }
              """,
            """
              class A {
                  void method() {
                      String key1 = "_" + "a";
                      String key2 = name() + "_" + "a";
                      String key3 = "m" + "_" + "a";
                      String key4 = "A" + "B" + "C";
                  }
                  String name() {
                      return "name";
                  }
              }
              """
          )
        );
    }

    @Test
    void objectsGrouping() {
        rewriteRun(
          java(
            """
              class A {
                  void method() {
                      String key0 = new StringBuilder().append(1 + 2).append(3).toString(); // "33"
                      String key1 = new StringBuilder().append(1.5).append(2).append(3).toString(); // "1.523"
                      String key2 = new StringBuilder("m").append(1 + 2).append(3).toString(); // "m33"
                      String key3 = new StringBuilder(10).append("_").append(1).toString(); // "_1"
                      String key4 = new StringBuilder("A" + "B").append(1 + 2).append(3).toString(); // "AB33"
                      String key5 = new StringBuilder().append(count()).append(1).toString(); // 101
                  }
                  int count() {
                      return 10;
                  }
              }
              """,
            """
              class A {
                  void method() {
                      String key0 = String.valueOf(1 + 2) + 3; // "33"
                      String key1 = "1.5" + 2 + 3; // "1.523"
                      String key2 = "m" + (1 + 2) + 3; // "m33"
                      String key3 = "_" + 1; // "_1"
                      String key4 = "A" + "B" + (1 + 2) + 3; // "AB33"
                      String key5 = String.valueOf(count()) + 1; // 101
                  }
                  int count() {
                      return 10;
                  }
              }
              """
          )
        );
    }
}
