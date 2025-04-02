/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StringLiteralReferenceTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new StringLiteralReference.Provider().getMatcher()
          .asVisitor(stringLiteralReference -> SearchResult.found(stringLiteralReference.getTree(), stringLiteralReference.getValue()))));
    }

    @SuppressWarnings("SpringXmlModelInspection")
    @Test
    @DocumentExample
    void xmlConfiguration() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                  String ref = "java.lang.String";
              }
              """,
            //language=java
            """
              class Test {
                  String ref = /*~~(java.lang.String)~~>*/"java.lang.String";
              }
              """
          )
        );
    }
}
