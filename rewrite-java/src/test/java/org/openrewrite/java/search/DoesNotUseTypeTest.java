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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DoesNotUseTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromYaml(
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.acme.RemoveUnusedImportsIfNotUsingString
            description: Test.
            preconditions:
              - org.openrewrite.java.search.DoesNotUseType:
                  fullyQualifiedTypeName: java.lang.String
                  includeImplicit: true
            recipeList:
              - org.openrewrite.java.RemoveUnusedImports
            """,
          "org.acme.RemoveUnusedImportsIfNotUsingString"
        );
    }

    @DocumentExample
    @Test
    void importRemovedWhenTypeNotFound() {
        rewriteRun(
          java(
            """
              import java.lang.StringBuilder;

              class Foo {
                  int bla = 123;
              }
              """,
            """
              class Foo {
                  int bla = 123;
              }
              """
          )
        );
    }

    @Test
    void importRetainedWhenTypeInUse() {
        rewriteRun(
          java(
            """
              import java.lang.StringBuilder;

              class Foo {
                  String bla = "bla";
              }
              """
          )
        );
    }
}
