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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SimplifyDurationCreationUnitsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifyDurationCreationUnits());
    }

    @Test
    void simplifyLiteralMillisToSeconds() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(5000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofSeconds(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMillisToMinutes() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(300000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMinutes(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMillisToHours() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(18000000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofHours(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMillisToDays() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(432000000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofDays(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMillisProduct() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(5 * 1000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofSeconds(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMillisProductWithWeirdFactors() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(5 * 5000);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofSeconds(25);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralSeconds() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofSeconds(300);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMinutes(5);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralMinutes() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMinutes(120);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofHours(2);
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralHours() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofHours(48);
              }
              """,
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofDays(2);
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeLiteralDays() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofDays(14);
              }
              """
          )
        );
    }

    @Test
    void doNotChangeSubSecondMillis() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  static Duration duration = Duration.ofMillis(5500);
              }
              """
          )
        );
    }

    /**
     * This is not a necessary constraint but should be considered carefully
     * before removing: when an operation other than multiplication is present
     * in one of these method invocations, what does it mean to the user?
     *
     * It's possible that simplifying units could obfuscate meaning.
     * It may be better to distribute the multiplicative factor change, rather
     * than simplify the expression; e.g. preferring
     *
     *     `Duration.ofMillis(1000 + 1000)`  --> `Duration.ofSeconds(1 + 1)`
     *
     * rather than
     *
     *     `Duration.ofMillis(1000 + 1000)`  --> `Duration.ofSeconds(2)`
     */
    @Test
    void doesNotChangeNonMultiplicationArithmetic() {
        rewriteRun(
          java(
            """
            import java.time.Duration;
                          
            public class Test {
                static Duration durationPlus = Duration.ofMillis(1000 + 1000);
                static Duration durationMinus = Duration.ofMillis(2000 - 1000);
                static Duration durationDivide = Duration.ofMillis(5000 / 5);
            }
            """
          )
        );
    }

    /**
     * This is not a necessary constraint; the recipe could simplify when there's a
     * constant multiplicative factor present (e.g. `Duration.ofSeconds(seconds)` here).
     * <p>
     * This test just documents the current behavior.
     */
    @Test
    void doesNotChangeNonConstantUnitCount() {
        rewriteRun(
          java(
              """
              import java.time.Duration;
                            
              public class Test {
                  int seconds = 30;
                  static Duration duration = Duration.ofMillis(1000 * seconds);
              }
              """
          )
        );
    }
}
