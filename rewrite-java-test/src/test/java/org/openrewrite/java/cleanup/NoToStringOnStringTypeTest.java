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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoToStringOnStringTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoToStringOnStringType());
    }

    @Test
    void doNotChangeOnObject() {
        rewriteRun(
          java(
            """
              class Test {
                  static String method(Object obj) {
                      return obj.toString();
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("StringOperationCanBeSimplified")
    void toStringOnString() {
        rewriteRun(
          java(
            """
              class Test {
                  static String method() {
                      return "hello".toString();
                  }
              }
              """,
            """
              class Test {
                  static String method() {
                      return "hello";
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("StringOperationCanBeSimplified")
    void toStringOnStringVariable() {
        rewriteRun(
          java(
            """
              class Test {
                  static String method(String str) {
                      return str.toString();
                  }
              }
              """,
            """
              class Test {
                  static String method(String str) {
                      return str;
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("StringOperationCanBeSimplified")
    void toStringOnMethodInvocation() {
        rewriteRun(
          java(
            """
              class Test {
                  static void method1() {
                      String str = method2().toString();
                  }

                  static String method2() {
                      return "";
                  }
              }
              """,
            """
              class Test {
                  static void method1() {
                      String str = method2();
                  }

                  static String method2() {
                      return "";
                  }
              }
              """
          )
        );
    }
}
