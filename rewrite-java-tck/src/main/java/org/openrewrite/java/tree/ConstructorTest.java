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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava21;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConstructorTest implements RewriteTest {

    @Test
    void noConstructor() {
        rewriteRun(
          java(
            """
              public class A {}
              """
          )
        );
    }

    @Test
    void defaultConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
              }
              """
          )
        );
    }

    @Test
    void multipleConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {}
              }
              """
          )
        );
    }

    @Test
    void thisCallingConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      this();
                  }
              }
              """
          )
        );
    }

    @Test
    void superCallingConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      super();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava21
    @Test
    void validationBeforeThisConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      if (a.equals("foo")) {
                          throw new RuntimeException();
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava21
    @Test
    void validationBeforeSuperConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A(String a) {
                      if (a.equals("foo")) {
                          throw new RuntimeException();
                      }
                      super();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava21
    @Test
    void assignmentBeforeThisConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  String stringA;
                  public A() {}
                  public A(String a) {
                      stringA = a;
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava21
    @Test
    void assignmentBeforeSuperConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  String stringA;
                  public A(String a) {
                      stringA = a;
                      super();
                  }
              }
              """
          )
        );
    }
}
