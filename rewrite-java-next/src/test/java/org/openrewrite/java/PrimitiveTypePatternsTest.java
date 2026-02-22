/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests for JEP 530: Primitive Types in Patterns, instanceof, and switch (Fourth Preview in JDK 26).
 */
class PrimitiveTypePatternsTest implements RewriteTest {

    @Test
    void primitivePatternCatchAll() {
        rewriteRun(
          java(
            """
              class Test {
                  String describe(int status) {
                      return switch (status) {
                          case 0 -> "okay";
                          case 1 -> "warning";
                          case 2 -> "error";
                          case int i -> "unknown status: " + i;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void primitivePatternWithGuard() {
        rewriteRun(
          java(
            """
              class Test {
                  String classify(int flights) {
                      return switch (flights) {
                          case 0 -> "none";
                          case 1 -> "once";
                          case int i when i >= 100 -> "frequent";
                          case int i -> "some: " + i;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void longPrimitivePatternInSwitch() {
        rewriteRun(
          java(
            """
              class Test {
                  String describe(long v) {
                      return switch (v) {
                          case 1L              -> "one";
                          case 2L              -> "two";
                          case 10_000_000_000L -> "ten billion";
                          case long x          -> "other: " + x;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void floatPrimitivePatternInSwitch() {
        rewriteRun(
          java(
            """
              class Test {
                  float compute(float v) {
                      return switch (v) {
                          case 0f -> 5f;
                          case float x when x == 1f -> 6f + x;
                          case float x -> 7f + x;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void primitiveInstanceof() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(int i) {
                      if (i instanceof byte b) {
                          System.out.println("fits in byte: " + b);
                      }
                  }
              }
              """
          )
        );
    }
}
