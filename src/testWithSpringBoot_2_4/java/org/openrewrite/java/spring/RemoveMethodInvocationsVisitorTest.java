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
package org.openrewrite.java.spring;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.internal.DocumentExample;
import org.openrewrite.test.AdHocRecipe;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.Assertions.java;

public class RemoveMethodInvocationsVisitorTest implements RewriteTest {

    private Recipe createRemoveMethodsRecipe(String... methods) {
        return new AdHocRecipe(null, null, null, () -> new RemoveMethodInvocationsVisitor(List.of(methods)), null, null, null, emptyList());
    }

    @DocumentExample
    @Test
    void removeFromEnd() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder toString()"))
          ,
          java(
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello")
                          .append(" ")
                          .append("World")
                          .reverse()
                          .append(" ")
                          .reverse()
                          .append("Yeah")
                          .toString();
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello")
                          .append(" ")
                          .append("World")
                          .reverse()
                          .append(" ")
                          .reverse()
                          .append("Yeah");
                  }
              }
              """
          )
        );
    }

    @Test
    void removeMultipleMethodsFromEnd() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder toString()", "java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello")
                          .append(" ")
                          .append("World")
                          .reverse()
                          .append(" ")
                          .reverse()
                          .append("Yeah")
                          .toString();
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.reverse()
                          .reverse();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeFromMiddle() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder reverse()")),
          java(
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello")
                          .append(" ")
                          .append("World")
                          .reverse()
                          .append(" ")
                          .reverse()
                          .append("Yeah")
                          .toString();
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello")
                          .append(" ")
                          .append("World")
                          .append(" ")
                          .append("Yeah")
                          .toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeEntireStatement() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello");
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                  }
              }
              """
          )
        );
    }

    @Test
    void keepSelectForAssignment() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      StringBuilder sb2 = sb.append("foo");
                      sb2.append("bar");
                      sb2.reverse();
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      StringBuilder sb = new StringBuilder();
                      StringBuilder sb2 = sb;
                      sb2.reverse();
                  }
              }
              """
          )
        );
    }

    @Test
    void chainedCallsAsParameter() {
        // language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              class Test {
                  void method() {
                      print(new StringBuilder()
                          .append("Hello")
                          .append(" ")
                          .append("World")
                          .reverse()
                          .append(" ")
                          .reverse()
                          .append("Yeah")
                          .toString());
                  }
                  void print(String str) {}
              }
              """,
            """
              class Test {
                  void method() {
                      print(new StringBuilder()
                          .reverse()
                          .reverse()
                          .toString());
                  }
                  void print(String str) {}
              }
              """
          )
        );
    }

    @Test
    void removeFromLambda() {
        // language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              import java.util.List;

              public class Test {
                  void method(List<String> names) {
                      names.forEach(name -> new StringBuilder()
                          .append("hello")
                          .append(" ")
                          .append(name)
                          .reverse()
                          .toString());
                  }
              }
              """,
            """
              import java.util.List;

              public class Test {
                  void method(List<String> names) {
                      names.forEach(name -> new StringBuilder()
                          .reverse()
                          .toString());
                  }
              }
              """
          )
        );
    }

    @Test
    void complexCase() {
        // language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              import java.util.List;
              import java.util.function.Consumer;

              public class Test {
                  void method(List<String> names) {
                      this.consume(s -> names.forEach(name -> {
                                  new StringBuilder()
                                      .append("hello")
                                      .append(" ")
                                      .append(name)
                                      .reverse()
                                      .toString();
                              }
                          )
                      ).toString();
                  }
                  StringBuilder consume(Consumer<String> consumer) {return new StringBuilder();}
              }
              """,
            """
              import java.util.List;
              import java.util.function.Consumer;

              public class Test {
                  void method(List<String> names) {
                      this.consume(s -> names.forEach(name -> {
                                  new StringBuilder()
                                      .reverse()
                                      .toString();
                              }
                          )
                      ).toString();
                  }
                  StringBuilder consume(Consumer<String> consumer) {return new StringBuilder();}
              }
              """
          )
        );
    }

    @Test
    void returnEmptyLambdaBody() {
        // language=java
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              import java.util.function.Consumer;

              public class Test {
                  public void method() throws Exception {
                      this.customize(sb -> sb
                          .append("Hello")
                      );
                  }

                  public void customize(Consumer<StringBuilder> securityContextCustomizer) {
                  }
              }
              """,
            """
              import java.util.function.Consumer;

              public class Test {
                  public void method() throws Exception {
                  }

                  public void customize(Consumer<StringBuilder> securityContextCustomizer) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaAssignment() {
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              import java.util.function.Consumer;
                          
              public class Test {
                  public void method() {
                      StringBuilder sb = new StringBuilder();
                      Consumer<String> consumer = name -> {
                          sb.append(name);
                      };
                      consumer.accept("hello");
                  }
              }
              """,
            """
              import java.util.function.Consumer;

              public class Test {
                  public void method() {
                      StringBuilder sb = new StringBuilder();
                      Consumer<String> consumer = name -> {
                      };
                      consumer.accept("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchBlocks() {
        rewriteRun(
          spec -> spec.recipe(createRemoveMethodsRecipe("java.lang.StringBuilder append(java.lang.String)")),
          java(
            """
              public class Test {
                  public void method() {
                      StringBuilder sb = new StringBuilder();
                      try {
                          sb.append("Hello");
                      } catch (Exception e) {
                          sb.append("Hello");
                      } finally {
                          sb.append("Hello");
                      }
                  }
              }
              """,
            """
              public class Test {
                  public void method() {
                      StringBuilder sb = new StringBuilder();
                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }
}
