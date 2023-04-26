/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.internal.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class NoFinalizedLocalVariablesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoFinalizedLocalVariables());
    }

    @DocumentExample
    @Test
    void removeFinal() {
        rewriteRun(
          java(
            """
              import java.util.function.Supplier;
              class T {
                  final int field = 0;
                  public void test(final String s) {
                      final int n = 0;
                      new Supplier<>() {
                          final int innerField = 0;
                          public String get() {
                              return s;
                          }
                      };
                  }
              }
              """,
            """
              import java.util.function.Supplier;
              class T {
                  final int field = 0;
                  public void test(String s) {
                      int n = 0;
                      new Supplier<>() {
                          final int innerField = 0;
                          public String get() {
                              return s;
                          }
                      };
                  }
              }
              """
          )
        );
    }
}
