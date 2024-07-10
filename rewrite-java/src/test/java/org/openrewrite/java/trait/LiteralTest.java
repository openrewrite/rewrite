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
package org.openrewrite.java.trait;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.trait.Traits.literal;

class LiteralTest implements RewriteTest {

    @Test
    void numericLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              literal().asVisitor(lit -> {
                  assertThat(lit.isNotNull()).isTrue();
                  // NOTE: Jackson's coercion config allows us to
                  // coerce various numeric literal types to an Integer
                  // if we like
                  return SearchResult.found(lit.getTree(),
                    lit.getValue(Integer.class).toString());
              })
            )
          ),
          java(
            """
              class Test {
                int n = 0;
                int d = 0.0;
              }
              """,
            """
              class Test {
                int n = /*~~(0)~~>*/0;
                int d = /*~~(0)~~>*/0.0;
              }
              """
          )
        );
    }

    @Test
    void arrayLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              literal().asVisitor(lit -> {
                  assertThat(lit.isNotNull()).isTrue();
                  return SearchResult.found(lit.getTree(),
                    String.join(",", lit.getValue(new TypeReference<List<String>>() {
                    })));
              })
            )
          ),
          java(
            """
              class Test {
                String[] s = new String[] { "a", "b", "c" };
                int[] n = new int[] { 0, 1, 2 };
              }
              """,
            """
              class Test {
                String[] s = /*~~(a,b,c)~~>*/new String[] { /*~~(a)~~>*/"a", /*~~(b)~~>*/"b", /*~~(c)~~>*/"c" };
                int[] n = /*~~(0,1,2)~~>*/new int[] { /*~~(0)~~>*/0, /*~~(1)~~>*/1, /*~~(2)~~>*/2 };
              }
              """
          )
        );
    }
}
