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
import org.openrewrite.java.MinimumJava21;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@MinimumJava21
class SwitchPatternMatchingTest implements RewriteTest {

    @Test
    void shouldParseJava21PatternSwitch() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                  String formatterPatternSwitch(Object obj) {
                      return switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l    -> String.format("long %d", l);
                          case Double d  -> String.format("double %f", d);
                          case String s  -> String.format("String %s", s);
                          default        -> obj.toString();
                      };
                  }
              }
              """
          ));
    }

    @Test
    void shouldSupportParsingNullSwitch() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                void fooBarWithNull(String s) {
                    switch (s) {
                        case null -> System.out.println("Oops");
                        case "Foo", "Bar" -> System.out.println("Great");
                        default -> System.out.println("Ok");
                    }
                }
              }
              """
          ));
    }

    @Test
    void shouldSupportParsingNullSwitchCombined() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                void fooBarWithNull(String s) {
                    switch (s) {
                        case "Foo", "Bar" -> System.out.println("Great");
                        case null, default -> System.out.println("Ok");
                    }
                }
              }
              """
          ));
    }

    @Test
    void shouldParseJava21EnumSupportInSwitch() {
        rewriteRun(
          java(
            //language=java
            """
              enum Coin  { HEADS, TAILS }

              class Test {
                  void switchEnum(Coin c) {
                      switch (c) {
                          case HEADS -> System.out.println("Heads");
                          case Coin.TAILS -> System.out.println("Tails");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldParseJava21ImprovedEnumSupportInSwitch() {
        rewriteRun(
          java(
            //language=java
            """
              sealed interface I permits Foo, Bar {}
              public enum Foo implements I { A, B }
              final class Bar implements I {}

              class Test {
                  void switchEnumExtendedType(I c) {
                      switch (c) {
                          case Foo.A -> System.out.println("It's Foo A");
                          case Foo.B -> System.out.println("It's Foo B");
                          case Bar b -> System.out.println("It's Bar");
                      }
                  }
              }
              """
          ));
    }

    @Test
    void shouldParseJava21SwitchWithRelaxedTypeRestrictions() {
        rewriteRun(
          java(
            //language=java
            """
              record Point(int i, int j) {}
              enum Color { RED, GREEN, BLUE; }

              class Test {
                  void typeTester(Object obj) {
                       switch (obj) {
                           case null     -> System.out.println("null");
                           case String s -> System.out.println("String");
                           case Color c  -> System.out.println("Color: " + c.toString());
                           case Point p  -> System.out.println("Record class: " + p.toString());
                           case int[] ia -> System.out.println("Array of ints of length" + ia.length);
                           default       -> System.out.println("Something else");
                       }
                  }
              }
              """
          ));
    }

    @Test
    void shouldParseJava21SwitchWithSpecialCases() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                  void integerTester(Integer i) {
                       switch (i) {
                           case -1, 1 -> System.out.println("special");
                           case Integer j when (j - 1) > -1 -> System.out.println("pos");
                           case Integer j -> System.out.println("others");
                       }
                  }
              }
              """
          ));
    }

    @Test
    void shouldParseRecordPatternMatchingInSwitch() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                  public interface Printable {}
                  record A(String A) implements Printable {}
                  record B(Integer B) implements Printable {}

                  void integerTester(Printable prt) {
                      switch (prt) {
                          case A(String a) -> System.out.println(a);
                          case B(Integer b) -> System.out.println(b);
                          default -> throw new IllegalStateException("Unexpected value: " + prt);
                      }
                  }
              }
              """
          ));
    }

}
