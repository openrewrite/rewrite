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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class GenerateGetterAndSetterVisitorTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1301")
    @Test
    void getterAndSetterForPrimitiveInteger() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GenerateGetterAndSetterVisitor<>("counter"))),
          java(
            """
              class T {
                  int counter;
              }
              """,
            """
              class T {
                  int counter;
                  public int getCounter() {
                      return counter;
                  }
                  public void setCounter(int counter) {
                      this.counter = counter;
                  }
              }
              """
          )
        );
    }

    @Test
    void getterAndSetterForNonPrimitive() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GenerateGetterAndSetterVisitor<>("size"))),
          java(
            """
              class T {
                  Float size;
              }
              """,
            """
              class T {
                  Float size;
                  public Float getSize() {
                      return size;
                  }
                  public void setSize(Float size) {
                      this.size = size;
                  }
              }
              """
          )
        );
    }

    @Test
    void getterAndSetterPrimitiveBoolean() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GenerateGetterAndSetterVisitor<>("valid"))),
          java(
            """
              class T {
                  boolean valid;
              }
              """,
            """
              class T {
                  boolean valid;
                  public boolean isValid() {
                      return valid;
                  }
                  public void setValid(boolean valid) {
                      this.valid = valid;
                  }
              }
              """
          )
        );
    }
}
