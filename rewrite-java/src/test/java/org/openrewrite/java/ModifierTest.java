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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ModifierTest implements RewriteTest {

    @Test
    public void modifierNoSpaceThenAnnotation() {
        rewriteRun(
                java(
                        """
                          public class Main {
                              public@jdk.jfr.Name("A") void test() {
                                  System.out.println("A");
                              }
                          }
                          """
                )
        );

    }

    @Test
    public void modifierNoSpaceThenMultipleAnnotation() {
        rewriteRun(
                java(
                        """
                          public class Main {
                              public@jdk.jfr.Name("A")@jdk.jfr.Label("test") void test() {
                                  System.out.println("A");
                              }
                          }
                          """
                )
        );

    }

    @Test
    public void multipleModifiersNoSpaceThenAnnotation() {
        rewriteRun(
                java(
                        """
                          public class Main {
                              public static@jdk.jfr.Name("A") void test() {
                                  System.out.println("A");
                              }
                          }
                          """
                )
        );
    }

    @Test
    void modifierWithSpaceThenAnnotation() {
        rewriteRun(
                java(
                        """
                          public class Main {
                              public static @jdk.jfr.Name("A") void test() {
                                  System.out.println("A");
                              }
                          }
                          """
                )
        );
    }
}

