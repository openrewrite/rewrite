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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class SimplifyBooleanReturnTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifyBooleanReturn());
    }

    @Test
    void simplifyBooleanReturn() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean ifNoElse() {
                      if (isOddMillis()) {
                          return true;
                      }
                      return false;
                  }
                  
                  static boolean isOddMillis() {
                      boolean even = System.currentTimeMillis() % 2 == 0;
                      if (even == true) {
                          return false;
                      }
                      else {
                          return true;
                      }
                  }
              }
              """,
            """
              public class A {
                  boolean ifNoElse() {
                      return isOddMillis();
                  }
                  
                  static boolean isOddMillis() {
                      boolean even = System.currentTimeMillis() % 2 == 0;
                      return !(even == true);
                  }
              }
              """
          )
        );
    }

    @Test
    void dontSimplifyToReturnUnlessLastStatement() {
        rewriteRun(
          java(
            """
              public class A {
                  public boolean absurdEquals(Object o) {
                      if(this == o) {
                          return true;
                      }
                      if(this == o) {
                          return true;
                      }
                      return false;
                  }
              }
              """,
            """
              public class A {
                  public boolean absurdEquals(Object o) {
                      if(this == o) {
                          return true;
                      }
                      return this == o;
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedIfsWithNoBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public boolean absurdEquals(Object o) {
                      if(this == o)
                          if(this == null) 
                              return true;
                      return false;
                  }
              }
              """
          )
        );
    }

    @Test
    void dontAlterWhenElseIfPresent() {
        rewriteRun(
          java(
            """
              public class A {
                  public boolean foo(int n) {
                      if (n == 1) {
                          return false;
                      } 
                      else if (n == 2) {
                          return true;
                      } 
                      else {
                          return false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dontAlterWhenElseContainsSomethingOtherThanReturn() {
        rewriteRun(
          java(
            """
              public class A {
                  public boolean foo(int n) {
                      if (n == 1) {
                          return true;
                      } 
                      else {
                          System.out.println("side effect");
                          return false;
                      } 
                  }
              }
              """
          )
        );
    }

    @Test
    void onlySimplifyToReturnWhenLastStatement() {
        rewriteRun(
          java(
            """
              import java.util.*;
              public class A {
                  public static boolean deepEquals(List<byte[]> l, List<byte[]> r) {
                      for (int i = 0; i < l.size(); ++i) {
                          if (!Arrays.equals(l.get(i), r.get(i))) {
                              return false;
                          }
                      }
                      return true;
                  }
              }
              """
          )
        );
    }

    @Test
    void wrapNotReturnsOfTernaryIfConditionsInParentheses() {
        rewriteRun(
          java(
            """
              public class A {
                  Object failure;
                  public boolean equals(Object o) {
                      if (failure != null ? !failure.equals(this.failure) : this.failure != null) {
                          return false;
                      }
                      return true;
                  }
              }
              """,
            """
              public class A {
                  Object failure;
                  public boolean equals(Object o) {
                      return !(failure != null ? !failure.equals(this.failure) : this.failure != null);
                  }
              }
              """
          )
        );
    }
}
