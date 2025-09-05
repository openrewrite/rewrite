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
import org.openrewrite.java.MinimumJava25;
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
    @MinimumJava25
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
    @MinimumJava25
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
    @MinimumJava25
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
    @MinimumJava25
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

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void localVariableBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      String validated = a.trim();
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void multipleStatementsBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a, int b) {
                      String processed = a.toUpperCase();
                      int adjusted = b + 10;
                      System.out.println("Preprocessing: " + processed);
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void tryBlockBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      try {
                          Integer.parseInt(a);
                      } catch (NumberFormatException e) {
                          throw new IllegalArgumentException("Not a number");
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void switchExpressionBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A(int value) {}
                  public A(String type) {
                      int value = switch (type) {
                          case "one" -> 1;
                          case "two" -> 2;
                          default -> 0;
                      };
                      this(value);
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void methodCallBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      validateInput(a);
                      this();
                  }
                  
                  private static void validateInput(String input) {
                      if (input == null) {
                          throw new NullPointerException();
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void nestedIfStatementsBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a, boolean flag) {
                      if (a != null) {
                          if (flag) {
                              System.out.println("Valid input");
                          } else {
                              throw new IllegalStateException("Flag must be true");
                          }
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void forLoopBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String[] args) {
                      for (String arg : args) {
                          if (arg == null) {
                              throw new IllegalArgumentException("Null argument found");
                          }
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void whileLoopBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(int count) {
                      int i = 0;
                      while (i < count) {
                          System.out.println("Count: " + i);
                          i++;
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void lambdaExpressionBeforeConstructor() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;
              
              public class A {
                  public A(String value) {}
                  public A(String input, boolean uppercase) {
                      Function<String, String> processor = uppercase ? 
                          s -> s.toUpperCase() : 
                          s -> s.toLowerCase();
                      String processed = processor.apply(input);
                      this(processed);
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void recordConstructorWithValidation() {
        rewriteRun(
          java(
            """
              public record Point(int x, int y) {
                  public Point {
                      if (x < 0 || y < 0) {
                          throw new IllegalArgumentException("Coordinates must be non-negative");
                      }
                  }
                  
                  public Point(String coords) {
                      String[] parts = coords.split(",");
                      int parsedX = Integer.parseInt(parts[0].trim());
                      int parsedY = Integer.parseInt(parts[1].trim());
                      this(parsedX, parsedY);
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void complexValidationWithEarlyReturn() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String input) {
                      if (input == null) {
                          this();
                          return;
                      }
                      if (input.isEmpty()) {
                          throw new IllegalArgumentException("Empty input");
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void superConstructorWithArgumentTransformation() {
        rewriteRun(
          java(
            """
              public class Parent {
                  public Parent(String value) {}
              }
              
              public class Child extends Parent {
                  public Child(int number) {
                      String formatted = String.format("Number: %d", number);
                      super(formatted);
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void blockStatementBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String input) {
                      {
                          String temp = input.trim();
                          if (temp.isEmpty()) {
                              throw new IllegalArgumentException();
                          }
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/513")
    @MinimumJava25
    @Test
    void assertStatementBeforeConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String input) {
                      assert input != null : "Input must not be null";
                      assert !input.isEmpty() : "Input must not be empty";
                      this();
                  }
              }
              """
          )
        );
    }
}
