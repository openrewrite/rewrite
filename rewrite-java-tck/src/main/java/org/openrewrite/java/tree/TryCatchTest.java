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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"EmptyTryBlock", "EmptyFinallyBlock"})
class TryCatchTest implements RewriteTest {

    @Test
    void catchRightPadding() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      try {
                          String foo;
                      } catch( Exception e ) {
                      //
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.Try.Catch visitCatch(J.Try.Catch c, Object o) {
                    assertThat(c.getParameter().getPadding().getTree().getAfter().getWhitespace())
                      .isEqualTo(" ");
                    return c;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void tryFinally() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      try {
                      }
                      finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchNoFinally() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      try {
                      }
                      catch(Throwable ignored) {
                      }
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
                      File f = new File("file.txt");
                      try (FileInputStream fis = new FileInputStream(f)) {
                      }
                      catch(IOException ignored) {
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessarySemicolon")
    @Test
    void tryWithResourcesSemiTerminated() {
        rewriteRun(
          java(
            """
                            import java.io.File;
              import java.io.FileInputStream;
              import java.io.IOException;
                            class Test {
                                void test() {
                                    File f = new File("file.txt");
                                    try (FileInputStream fis = new FileInputStream(f) ; ) {
                                    }
                                    catch(IOException ignored) {
                                    }
                                }
                            }
                            """
          )
        );
    }

    @Test
    void multiCatch() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void test() {
                      File f = new File("file.txt");
                      try(FileInputStream fis = new FileInputStream(f)) {}
                      catch(FileNotFoundException | RuntimeException ignored) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleResources() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void test() {
                      File f = new File("file.txt");
                      try(FileInputStream fis = new FileInputStream(f); FileInputStream fis2 = new FileInputStream(f)) {}
                      catch(RuntimeException | IOException ignored) {}
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"CatchMayIgnoreException", "TryWithIdenticalCatches"})
    @Test
    void tryCatchFinally() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      try {}
                      catch(Exception e) {}
                      catch(RuntimeException e) {}
                      catch(Throwable t) {}
                      finally {}
                  }
              }
              """
          )
        );
    }

    @MinimumJava11
    @Issue("https://github.com/openrewrite/rewrite/issues/763")
    @Test
    void tryWithResourcesIdentifier() {
        rewriteRun(
          java(
            """
              import java.io.InputStream;
              class A {
                  void test() {
                      InputStream in;
                      try (in) {
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava11
    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    void tryWithResourcesIdentifierAndVariables() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.FileInputStream;
              import java.util.Scanner;
                            
              class A {
                  void a() throws Exception {
                      FileInputStream fis = new FileInputStream("file.txt");
                      try (fis; Scanner sc = new Scanner("")) {
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessarySemicolon")
    @MinimumJava11
    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    void tryWithResourcesIdentifierAndSemicolon() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.FileInputStream;
              import java.util.Scanner;

              class A {
                  void a() throws Exception {
                      FileInputStream fis = new FileInputStream("file.txt");
                      try (fis;) {
                      }
                  }
              }
              """
          )
        );
    }
}
