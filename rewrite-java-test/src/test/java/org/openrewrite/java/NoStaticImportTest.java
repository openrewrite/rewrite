/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoStaticImportTest implements RewriteTest {

    @Test
    void replaceWithNoStaticImports() {
        rewriteRun(
          spec -> spec.recipe(new NoStaticImport("java.util.Collections emptyList()")),
          java(
            """
              import static java.util.Collections.emptyList;
              class Test {
                  void test() {
                      Object o = emptyList();
                  }
              }
              """,
            """
              import java.util.Collections;
                              
              class Test {
                  void test() {
                      Object o = Collections.emptyList();
                  }
              }
              """
          )
        );
    }

    @Nested
    class Retain {

        public static final NoStaticImport NO_STATIC_IMPORT = new NoStaticImport("*..* *(..)");

        @Test
        void verifyInnerCallsAreNotUpdated() {
            rewriteRun(
              spec -> spec.recipe(NO_STATIC_IMPORT),
              java("""
                package org.openrewrite.java;

                public class TestNoStaticImport {

                    public static void method0() {
                    }

                    public static void method1() {
                        method0();
                    }
                }
                """));
        }

        @Test
        void superCallsNotChanged() {
            rewriteRun(
              spec -> spec.recipe(NO_STATIC_IMPORT),
              java("""
                package org.openrewrite.java;

                public class TestNoStaticImport {

                    public TestNoStaticImport() {
                        super();
                    }
                }
                """));
        }

        @Test
        void innerClassCallingOuterClassMethod() {
            rewriteRun(
              spec -> spec.recipe(NO_STATIC_IMPORT),
              java("""
                package org.openrewrite.java;

                public class TestNoStaticImport {
                    public static void method0() {
                    }

                    public class InnerClass {
                        public void method1() {
                            method0();
                        }
                    }
                }
                """));
        }

        @Test
        void staticInnerClassCallingOuterClassMethod() {
            rewriteRun(
              spec -> spec.recipe(NO_STATIC_IMPORT),
              java("""
                package org.openrewrite.java;

                public class TestNoStaticImport {
                    public static void method0() {
                    }

                    public static class InnerClass {
                        public void method1() {
                            method0();
                        }
                    }
                }
                """));
        }

        @Test
        void outerClassCallingInnerClassMethod() {
            rewriteRun(
              spec -> spec.recipe(NO_STATIC_IMPORT),
              java("""
                package org.openrewrite.java;

                class TestNoStaticImport {
                    public static void method0() {
                        InnerClass.method1();
                    }
                            
                    public class InnerClass {
                        public static void method1() {
                        }
                    }
                }
                """));
        }
    }
}