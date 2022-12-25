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
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;

class CompareEnumWithEqualityOperatorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CompareEnumsWithEqualityOperator());
    }

    SourceSpecs enumA = java(
      """
        package a;
        public enum A {
            FOO, BAR, BUZ
        }
        """
    );

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void changeEnumEquals() {
        rewriteRun(
          enumA,
          java(
            """
              import a.A;
              class Test {
                  void method(A arg0) {
                      if (A.FOO.equals(arg0)) {
                      }
                      if (arg0.equals(A.FOO)) {
                      }
                  }
              }
              """,
            """
              import a.A;
              class Test {
                  void method(A arg0) {
                      if (A.FOO == arg0) {
                      }
                      if (arg0 == A.FOO) {
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void changeEnumNotEquals() {
        rewriteRun(
          enumA,
          java(
            """
              import a.A;
              class Test {
                  void method(A arg0) {
                      if (!A.FOO.equals(arg0)) {
                      }
                      if (!arg0.equals(A.FOO)) {
                      }
                  }
              }
              """,
            """
              import a.A;
              class Test {
                  void method(A arg0) {
                      if (A.FOO != arg0) {
                      }
                      if (arg0 != A.FOO) {
                      }
                  }
              }
              """
          )
        );
    }
}
