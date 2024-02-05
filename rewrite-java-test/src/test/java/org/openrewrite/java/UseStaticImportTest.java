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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseStaticImportTest implements RewriteTest {
    @Test
    void replaceWithStaticImports() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("asserts.Assert assert*(..)")),
          java(
            """
              package asserts;

              public class Assert {
                  public static void assertTrue(boolean b) {}
                  public static void assertFalse(boolean b) {}
                  public static void assertEquals(int m, int n) {}
              }
              """
          ),
          java(
            """
              package test;
                            
              import asserts.Assert;
                            
              class Test {
                  void test() {
                      Assert.assertTrue(true);
                      Assert.assertEquals(1, 2);
                      Assert.assertFalse(false);
                  }
              }
              """,
            """
              package test;
                            
              import static asserts.Assert.*;
                            
              class Test {
                  void test() {
                      assertTrue(true);
                      assertEquals(1, 2);
                      assertFalse(false);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3705")
    @Test
    void ignoreMethodsWithTypeParameter() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.Collections emptyList()")),
          java(
            """
            import java.util.Collections;
            import java.util.List;

            public class Reproducer {
                public void methodWithTypeParameter() {
                    List<Object> list = Collections.<Object>emptyList();
                }
            }
            """
          )
        );
    }

    @Test
    void sameMethodLocallyNoStaticImport() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.Collections emptyList()")),
          java(
            """
            import java.util.Collections;
            import java.util.List;

            public class SameMethodNameLocally {
                public void avoidCollision() {
                    List<Object> list = Collections.emptyList();
                }
                
                private int emptyList(String canHaveDifferentArguments) {
                }
            }
            """
          )
        );
    }

    @Test
    void doReplaceWhenWildcard() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.Collections *()")),
          java(
            """
            import java.util.Collections;
            import java.util.List;

            class SameMethodNameLocally {
                void avoidCollision() {
                    List<Object> list = Collections.emptyList();
                }
            }
            """,
            """
            import java.util.List;
            
            import static java.util.Collections.emptyList;

            class SameMethodNameLocally {
                void avoidCollision() {
                    List<Object> list = emptyList();
                }
            }
            """
          )
        );
    }

    @DocumentExample
    @Test
    void junit5Assertions() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
            .recipe(new UseStaticImport("org.junit.jupiter.api.Assertions assert*(..)")),
          java(
            """
              package org.openrewrite;

              import org.junit.jupiter.api.Test;
              import org.junit.jupiter.api.Assertions;

              class SampleTest {
                  @Test
                  void sample() {
                      Assertions.assertEquals(42, 21*2);
                  }
              }
              """,
            """
              package org.openrewrite;

              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.assertEquals;

              class SampleTest {
                  @Test
                  void sample() {
                      assertEquals(42, 21*2);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3663")
    @Test
    void javadocLinkUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.Collections emptyList()")),
          java(
            """
            import java.util.Collections;
            import java.util.List;

            public class WithJavadoc {
                /**
                 * This method uses {@link Collections#emptyList()}.
                 */
                public void mustNotChangeTheJavadocAbove() {
                    List<Object> list = Collections.emptyList();
                }
            }
            """,
            """
            import java.util.Collections;
            import java.util.List;

            import static java.util.Collections.emptyList;

            public class WithJavadoc {
                /**
                 * This method uses {@link Collections#emptyList()}.
                 */
                public void mustNotChangeTheJavadocAbove() {
                    List<Object> list = emptyList();
                }
            }
            """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3661")
    void staticCall() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.function.Predicate *(..)")),
          //language=java
          java(
            """
              import java.util.function.Predicate;
              public class Reproducer {
                  void reproduce() {
                      Predicate<Object> predicate = x -> false;
                      Predicate staticPredicate = Predicate.not(predicate);
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              import static java.util.function.Predicate.not;

              public class Reproducer {
                  void reproduce() {
                      Predicate<Object> predicate = x -> false;
                      Predicate staticPredicate = not(predicate);
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3661")
    void nonStaticCall() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("java.util.function.Predicate *(..)")),
          //language=java
          java(
            """
              import java.util.function.Predicate;
              public class Reproducer {
                  void reproduce() {
                      Predicate<Object> predicate = x -> false;
                      boolean nonStatic = predicate.test(null);
                  }
              }
              """
          )
        );
    }
}
