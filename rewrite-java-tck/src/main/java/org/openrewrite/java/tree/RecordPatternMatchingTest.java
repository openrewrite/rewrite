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
class RecordPatternMatchingTest implements RewriteTest {

    @Test
    void shouldParseJava21PatternMatchForRecords() {
        rewriteRun(
          java(
            //language=java
            """
              record Point(int x, int y) {}
              class Test {
                  void printSum(Object obj) {
                      if (obj instanceof Point(int x, int y)) {
                          System.out.println(x+y);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldParseRecordPatternWithVarKeyword() {
        rewriteRun(
          java(
            //language=java
            """
              record Point(int x, int y) {}
              class Test {
                  void printSum(Object obj) {
                      if (obj instanceof Point(var x, var y)) {
                          System.out.println(x + y);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldParseJava21NestedPatternMatchForRecords() {
        rewriteRun(
          java(
            //language=java
            """
              record Point(int x, int y) {}
              enum Color { RED, GREEN, BLUE }
              record ColoredPoint(Point p, Color c) {}
              record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
              class Test {
                  void printColorOfUpperLeftPoint(Rectangle r) {
                      if (r instanceof Rectangle(ColoredPoint(Point p, Color c),
                                                 ColoredPoint lr)) {
                          System.out.println(c);
                      }
                  }
              }
              """
          )
        );
    }

}
