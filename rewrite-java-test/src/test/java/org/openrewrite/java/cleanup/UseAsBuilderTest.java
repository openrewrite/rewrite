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

class UseAsBuilderTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseAsBuilder("Buildable.Builder", true, "Buildable builder()"));
    }

    @Test
    void useAsBuilder() {
        rewriteRun(
          java(
            """
              class Buildable {
                  public static Builder builder() {
                      return new Builder();
                  }
              
                  public static class Builder {
                      public Builder option(String option) {
                          return this;
                      }
                  }
              }
              """
          ),
          java(
            """
              class Test {
                  void test() {
                      int a = 0;
                      Buildable.Builder builder = Buildable.builder();
                      String b = "rewrite-java";
                      int c = 0;
                      builder = builder.option(b);
                      int d = 0;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int a = 0;
                      String b = "rewrite-java";
                      int c = 0;
                      Buildable.Builder builder = Buildable.builder()
                              .option(b);
                      int d = 0;
                  }
              }
              """
          )
        );
    }
}
