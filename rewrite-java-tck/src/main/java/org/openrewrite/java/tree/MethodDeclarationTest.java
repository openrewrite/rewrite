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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava25;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MethodDeclarationTest implements RewriteTest {

    @Test
    void defaultValue() {
        rewriteRun(
          java(
            """
              public @interface A {
                  String foo() default "foo";
              }
              """
          )
        );
    }

    @Test
    void constructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() { }
              }
              """
          )
        );
    }

    @Test
    void typeArguments() {
        rewriteRun(
          java(
            """
              class Test {
                  public <P, R> R foo(P p, String s, String... args) {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void interfaceMethodDecl() {
        rewriteRun(
          java(
            """
              public interface A {
                  String getName() ;
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void methodThrows() {
        rewriteRun(
          java(
            """
              class Test {
                  public void foo()  throws Exception { }
              }
              """
          )
        );
    }

    @Test
    void nativeModifier() {
        rewriteRun(
          java(
            """
              class Test {
                  public native void foo();
              }
              """
          )
        );
    }

    @Test
    void methodWithSuffixMultiComment() {
        rewriteRun(
          java(
            """
              class Test {
                  public void foo() { }/*Comments*/
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Nested
    class InstanceMainMethods implements RewriteTest {

        @Test
        void instanceMainMethodWithNoParameters() {
            rewriteRun(
              java(
                """
                  class HelloWorld {
                      void main() {
                          System.out.println("Hello, World!");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void instanceMainMethodWithStringArrayParameter() {
            rewriteRun(
              java(
                """
                  class HelloWorld {
                      void main(String[] args) {
                          System.out.println("Hello, World!");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void instanceMainMethodWithInstanceFields() {
            rewriteRun(
              java(
                """
                  class Counter {
                      private int count = 0;

                      void main() {
                          count++;
                          System.out.println("Count: " + count);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void instanceMainMethodCallingInstanceMethods() {
            rewriteRun(
              java(
                """
                  class Greeter {
                      void main() {
                          greet("World");
                      }

                      void greet(String name) {
                          System.out.println("Hello, " + name + "!");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void staticMainMethodStillSupported() {
            rewriteRun(
              java(
                """
                  class TraditionalMain {
                      public static void main(String[] args) {
                          System.out.println("Traditional main method");
                      }
                  }
                  """
              )
            );
        }
    }
}
