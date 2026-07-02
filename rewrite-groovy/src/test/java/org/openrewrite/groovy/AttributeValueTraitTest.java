/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

/**
 * Pins the {@link org.openrewrite.java.trait.AttributeValue} behavior on Groovy sources.
 * Groovy never populates {@link org.openrewrite.java.tree.JavaType.Annotation} element
 * values, so constant folding degrades to {@code null}; Groovy list literals are
 * {@code G.ListLiteral}, not {@code J.NewArray}, so they classify as
 * {@code Kind.EXPRESSION} without element normalization.
 */
class AttributeValueTraitTest implements RewriteTest {

    @Test
    void implicitValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getDefaultAttributeValue(null)
                .map(v -> v.getKind() + ":" + v.getConstantValue() + ":" + v.getName())
                .orElse("missing"))))),
          groovy(
            """
              @interface Example {
                  String value()
              }

              @Example("x")
              class Test {
              }
              """,
            """
              @interface Example {
                  String value()
              }

              /*~~(LITERAL:x:value)~~>*/@Example("x")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void namedAttribute() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("name")
                .map(v -> v.getKind() + ":" + v.getConstantValue())
                .orElse("missing"))))),
          groovy(
            """
              @interface Example {
                  String name()
              }

              @Example(name = "x")
              class Test {
              }
              """,
            """
              @interface Example {
                  String name()
              }

              /*~~(LITERAL:x)~~>*/@Example(name = "x")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void listLiteralIsAnOpaqueExpression() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("tags")
                .map(v -> v.getKind() + ":elements=" + v.getElements().size())
                .orElse("missing"))))),
          groovy(
            """
              @interface Example {
                  String[] tags()
              }

              @Example(tags = ["a", "b"])
              class Test {
              }
              """,
            """
              @interface Example {
                  String[] tags()
              }

              /*~~(EXPRESSION:elements=1)~~>*/@Example(tags = ["a", "b"])
              class Test {
              }
              """
          )
        );
    }

    @Test
    void constantReferenceDoesNotFold() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("name")
                .map(v -> v.getKind() + ":" + v.getConstantValue())
                .orElse("missing"))))),
          groovy(
            """
              @interface Example {
                  String name()
              }

              class Constants {
                  static final String NAME = "n"
              }

              @Example(name = Constants.NAME)
              class Test {
              }
              """,
            """
              @interface Example {
                  String name()
              }

              class Constants {
                  static final String NAME = "n"
              }

              /*~~(CONSTANT_REFERENCE:null)~~>*/@Example(name = Constants.NAME)
              class Test {
              }
              """
          )
        );
    }
}
