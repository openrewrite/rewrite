/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("FunctionName")
class FindLocalTaintFlowTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new FindLocalFlowPaths<>(new LocalTaintFlowSpec<J.MethodInvocation, Expression>() {
            @Override
            public boolean isSource(J.MethodInvocation methodInvocation, Cursor cursor) {
                return methodInvocation.getSimpleName().equals("source");
            }

            @Override
            public boolean isSink(Expression expression, Cursor cursor) {
                return true;
            }

            @Override
            public boolean isSanitizer(Expression expression, Cursor cursor) {
                if(expression instanceof J.Binary binary) {
                    return binary.getOperator() == J.Binary.Type.Addition &&
                           binary.getRight() instanceof J.Literal &&
                           Objects.equals(((J.Literal) binary.getRight()).getValue(), "sanitizer");
                }
                return false;
            }
        }))).expectedCyclesThatMakeChanges(1).cycles(1);
    }

    @Test
    void taintTrackingThroughStringManipulations() {
        rewriteRun(
          java(
            """
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = source();
                      String o = n.substring(0, 3);
                      String p = o.toUpperCase();
                      System.out.println(p);
                  }
              }
              """,
            """
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = /*~~>*/source();
                      String o = /*~~>*//*~~>*/n.substring(0, 3);
                      String p = /*~~>*//*~~>*/o.toUpperCase();
                      System.out.println(/*~~>*/p);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintTrackingThroughFileManipulations() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.nio.file.Path;
              class Test {
                  File source() { return null; }
                  void test() {
                      {
                          File n = source();
                          String o = n.getAbsolutePath();
                          String p = o.toUpperCase();
                          System.out.println(p);
                      }
                      // To Path Type
                      {
                          File n = source();
                          Path o = n.toPath();
                          File p = o.toFile();
                          System.out.println(p);
                      }
                  }
              }
              """,
            """
              import java.io.File;
              import java.nio.file.Path;
              class Test {
                  File source() { return null; }
                  void test() {
                      {
                          File n = /*~~>*/source();
                          String o = /*~~>*//*~~>*/n.getAbsolutePath();
                          String p = /*~~>*//*~~>*/o.toUpperCase();
                          System.out.println(/*~~>*/p);
                      }
                      // To Path Type
                      {
                          File n = /*~~>*/source();
                          Path o = /*~~>*//*~~>*/n.toPath();
                          File p = /*~~>*//*~~>*/o.toFile();
                          System.out.println(/*~~>*/p);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void taintTrackingThroughFileConstructor() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.net.URI;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = source();
                      File o = new File(n);
                      URI p = o.toURI();
                      System.out.println(p);
                  }
              }
              """,
            """
              import java.io.File;
              import java.net.URI;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = /*~~>*/source();
                      File o = /*~~>*/new File(/*~~>*/n);
                      URI p = /*~~>*//*~~>*/o.toURI();
                      System.out.println(/*~~>*/p);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintTrackingThroughStringJoin() {
        rewriteRun(
          java(
            """
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = source();
                      String o = String.join(", ", n);
                      String p = String.join(o, ", ");
                      String q = String.join(" ", "hello", p);
                      System.out.println(q);
                  }
              }
              """,
            """
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = /*~~>*/source();
                      String o = /*~~>*/String.join(", ", /*~~>*/n);
                      String p = /*~~>*/String.join(/*~~>*/o, ", ");
                      String q = /*~~>*/String.join(" ", "hello", /*~~>*/p);
                      System.out.println(/*~~>*/q);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintTrackingThroughStringAppending() {
        rewriteRun(
          java(
            """
              import java.io.File;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = source();
                      String o = "hello " + n ;
                      String p = o + " world";
                      String q = p + File.separatorChar;
                      String r = q + true;
                      System.out.println(r);
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = /*~~>*/source();
                      String o = /*~~>*/"hello " + /*~~>*/n ;
                      String p = /*~~>*//*~~>*/o + " world";
                      String q = /*~~>*//*~~>*/p + File.separatorChar;
                      String r = /*~~>*//*~~>*/q + true;
                      System.out.println(/*~~>*/r);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintStopsAtSanitizer() {
        rewriteRun(
          java(
            """
              import java.io.File;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = source();
                      String o = "hello " + n ;
                      String p = o + " world";
                      String q = p + "sanitizer";
                      String r = q + true;
                      System.out.println(r);
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  String source() { return null; }
                  void test() {
                      String n = /*~~>*/source();
                      String o = /*~~>*/"hello " + /*~~>*/n ;
                      String p = /*~~>*//*~~>*/o + " world";
                      String q = /*~~>*/p + "sanitizer";
                      String r = q + true;
                      System.out.println(r);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintTrackingThroughTryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.InputStream;
              class Test {
                  InputStream source() { return null; }
                  void test() {
                      try (InputStream source = source()) {
                          System.out.println(source.read());
                      }
                  }
              }
              """,
            """
              import java.io.InputStream;
              class Test {
                  InputStream source() { return null; }
                  void test() {
                      try (InputStream source = /*~~>*/source()) {
                          System.out.println(/*~~>*/source.read());
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Test
    void taintTrackingThroughTry() {
        rewriteRun(
          java(
            """
              import java.io.InputStream;
              class Test {
                  InputStream source() { return null; }
                  void test() {
                      InputStream source = source();
                      try {
                          System.out.println(source.read());
                      } finally {
                          source.close();
                      }
                  }
              }
              """,
            """
              import java.io.InputStream;
              class Test {
                  InputStream source() { return null; }
                  void test() {
                      InputStream source = /*~~>*/source();
                      try {
                          System.out.println(/*~~>*/source.read());
                      } finally {
                          /*~~>*/source.close();
                      }
                  }
              }
              """
          )
        );
    }
}
