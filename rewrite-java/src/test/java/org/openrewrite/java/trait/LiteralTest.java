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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class LiteralTest implements RewriteTest {

    @DocumentExample
    @Test
    void numericLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit -> {
                  assertThat(lit.isNotNull()).isTrue();
                  // NOTE: Jackson's coercion config allows us to
                  // coerce various numeric literal types to an Integer
                  // if we like
                  return SearchResult.found(lit.getTree(),
                    requireNonNull(lit.getValue(Integer.class)).toString());
              })
            )
          ),
          java(
            """
              class Test {
                int n = 0;
                double d = 0.0;
              }
              """,
            """
              class Test {
                int n = /*~~(0)~~>*/0;
                double d = /*~~(0)~~>*/0.0;
              }
              """
          )
        );
    }

    @Test
    void nullLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit ->
                SearchResult.found(lit.getTree(),
                  "null=" + lit.isNull() + ":string=" + lit.getString() + ":value=" + lit.getValue(String.class)))
            )
          ),
          java(
            """
              class Test {
                String s = null;
              }
              """,
            """
              class Test {
                String s = /*~~(null=true:string=null:value=null)~~>*/null;
              }
              """
          )
        );
    }

    @Test
    void stringAccessors() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit ->
                SearchResult.found(lit.getTree(),
                  "string=" + lit.getString() + ":strings=" + String.join(",", lit.getStrings())))
            )
          ),
          java(
            """
              class Test {
                String s = "a";
                String[] t = { "b" };
              }
              """,
            """
              class Test {
                String s = /*~~(string=a:strings=a)~~>*/"a";
                String[] t = /*~~(string=null:strings=b)~~>*/{ /*~~(string=b:strings=b)~~>*/"b" };
              }
              """
          )
        );
    }

    @Test
    void nestedArrayLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit ->
                SearchResult.found(lit.getTree(), String.valueOf(lit.getValue(Object.class))))
            )
          ),
          java(
            """
              class Test {
                int[][] n = { { 1, 2 }, { 3 } };
              }
              """,
            """
              class Test {
                int[][] n = /*~~([[1, 2], [3]])~~>*/{ /*~~([1, 2])~~>*/{ /*~~(1)~~>*/1, /*~~(2)~~>*/2 }, /*~~([3])~~>*/{ /*~~(3)~~>*/3 } };
              }
              """
          )
        );
    }

    @Test
    void nonCoercibleValueThrows() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().mapper(new ObjectMapper()).asVisitor(lit -> {
                  // diverges from AttributeValue.getValue, which returns null instead of throwing
                  try {
                      return SearchResult.found(lit.getTree(), "coerced=" + lit.getValue(Integer.class));
                  } catch (IllegalArgumentException e) {
                      return SearchResult.found(lit.getTree(), "throws");
                  }
              })
            )
          ),
          java(
            """
              class Test {
                String s = "abc";
              }
              """,
            """
              class Test {
                String s = /*~~(throws)~~>*/"abc";
              }
              """
          )
        );
    }

    @Test
    void emptyArrayLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit -> {
                  assertThat(lit.isArray()).isTrue();
                  assertThat(lit.isNotNull()).isTrue();
                  assertThat(lit.getStrings()).isEmpty();
                  return SearchResult.found(lit.getTree());
              })
            )
          ),
          java(
            """
              class Test {
                String[] s = {};
                int[] n = new int[] {};
              }
              """,
            """
              class Test {
                String[] s = /*~~>*/{};
                int[] n = /*~~>*/new int[] {};
              }
              """
          )
        );
    }

    @Test
    void arrayLiteral() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new Literal.Matcher().asVisitor(lit -> {
                  assertThat(lit.isNotNull()).isTrue();
                  return SearchResult.found(lit.getTree(),
                    String.join(",", lit.getStrings()));
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
