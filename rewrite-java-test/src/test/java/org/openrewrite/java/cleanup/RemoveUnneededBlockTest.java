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

@SuppressWarnings({"UnusedLabel", "StatementWithEmptyBody", "Convert2Diamond", "ConstantConditions", "ClassInitializerMayBeStatic"})
class RemoveUnneededBlockTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnneededBlock());
    }

    @Test
    void doNotChangeMethod() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeLabeledBlock() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      testLabel: {
                          System.out.println("hello!");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeEmptyIfBlock() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      if(true) { }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveDoubleBraceInitBlocksInMethod() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              public class T {
                  public void whenInitializeSetWithDoubleBraces_containsElements() {
                      Set<String> countries = new HashSet<String>() {
                          {
                             add("a");
                             add("b");
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveDoubleBraceInitBlocks() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              public class T {
                  final Set<String> countries = new HashSet<String>() {
                      {
                         add("a");
                         add("b");
                      }
                  };
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveObjectArrayInitializer() {
        rewriteRun(
          java(
            """
              public class A {
                  Object[] a = new Object[] {
                      "a",
                      "b"
                  };
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveObjectArrayArrayInitializer() {
        rewriteRun(
          java(
            """
              public class A {
                  Object[][] a = new Object[][] {
                      { "a", "b" },
                      { "c", "d" }
                  };
              }
              """
          )
        );
    }

    @Test
    void simplifyNestedBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  void test() {
                      {
                          System.out.println("hello!");
                      }
                  }
              }
              """,
            """
              public class A {
                  void test() {
                      System.out.println("hello!");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyDoublyNestedBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  void test() {
                      {
                           { System.out.println("hello!"); }
                      }
                  }
              }
              """,
            """
              public class A {
                  void test() {
                      System.out.println("hello!");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyBlockNestedInIfBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  void test() {
                      if (true) {
                           { System.out.println("hello!"); }
                      }
                  }
              }
              """,
            """
              public class A {
                  void test() {
                      if (true) {
                          System.out.println("hello!");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyBlockInStaticInitializerIfBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  static {
                      {
                           {
                              System.out.println("hello static!");
                              System.out.println("goodbye static!");
                           }
                      }
                  }

                  {
                      {
                          System.out.println("hello init!");
                          System.out.println("goodbye init!");
                      }
                  }
              }
              """,
            """
              public class A {
                  static {
                      System.out.println("hello static!");
                      System.out.println("goodbye static!");
                  }

                  {
                      System.out.println("hello init!");
                      System.out.println("goodbye init!");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyCraziness() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              public class A {
                  static {
                      {
                           new HashSet<String>() {
                              {
                                  add("a");
                                  add("b");
                                  {
                                      System.out.println("hello static!");
                                      System.out.println("goodbye static!");
                                  }
                              }
                           };
                      }
                  }

                  {
                      {
                           new HashSet<String>() {
                              {
                                  add("a");
                                  add("b");
                                  {
                                      System.out.println("hello init!");
                                      System.out.println("goodbye init!");
                                  }
                              }
                           };
                      }
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              public class A {
                  static {
                      new HashSet<String>() {
                          {
                              add("a");
                              add("b");
                              System.out.println("hello static!");
                              System.out.println("goodbye static!");
                          }
                      };
                  }

                  {
                      new HashSet<String>() {
                          {
                              add("a");
                              add("b");
                              System.out.println("hello init!");
                              System.out.println("goodbye init!");
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyDoesNotFormatSurroundingCode() {
        rewriteRun(
          java(
            """
              public class A {
                  static {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      {
                          System.out.println("hello static!");
                      }
                  }
              }
              """,
            """
              public class A {
                  static {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      System.out.println("hello static!");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyDoesNotFormatInternalCode() {
        rewriteRun(
          java(
            """
              public class A {
                  static {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      {
                          System.out.println("hello!");
                          System.out.println( "world!" );
                      }
                  }
              }
              """,
            """
              public class A {
                  static {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      System.out.println("hello!");
                      System.out.println("world!");
                  }
              }
              """
          )
        );
    }
}
