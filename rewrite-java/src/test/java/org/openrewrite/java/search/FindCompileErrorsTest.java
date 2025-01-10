/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.table.CompileErrors;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.TypeValidation.all;

class FindCompileErrorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new FindCompileErrors());
    }

    @DocumentExample
    @Test
    void javaVisitorHandlesErroneousNodes() {
        rewriteRun(
          spec -> spec.dataTable(CompileErrors.Row.class,
            rows -> assertThat(rows).singleElement().satisfies(row -> {
                assertThat(row.getSourceFile()).isEqualTo("A.java");
                assertThat(row.getCode()).isEqualTo("\n        owner");
            })).typeValidationOptions(all().erroneous(false)),
          java(
            """
              class A {
                  void test() {
                      owner
                  }
              }
              """,
            """
              class A {
                  void test() {
                      /*~~>*/owner
                  }
              }
              """
          )
        );
    }
}
