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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

/**
 * The {@link org.openrewrite.java.trait.AttributeValue} behavior on Groovy sources,
 * asserting the same semantics the trait has on javac-attributed Java sources.
 * Tests annotated {@link ExpectedToFail} document known Groovy parser/type-mapping
 * gaps: property-access references carry no {@code fieldType}
 * ({@code GroovyParserVisitor#visitPropertyExpression}), no
 * {@code JavaType.Annotation} element values are built (no constant folding), and
 * list literals are {@code G.ListLiteral}, not {@code J.NewArray}.
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

    @ExpectedToFail("Groovy list literals are G.ListLiteral, not J.NewArray; getElements() cannot normalize them from rewrite-java")
    @Test
    void listLiteralArray() {
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

              /*~~(ARRAY:elements=2)~~>*/@Example(tags = ["a", "b"])
              class Test {
              }
              """
          )
        );
    }

    @ExpectedToFail("GroovyParserVisitor#visitPropertyExpression attaches no type to the idiomatic bare class reference")
    @Test
    void bareClassReference() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("type")
                .map(v -> v.getKind() + ":field=" + (v.getReferencedField() != null) + ":class=" + (v.getClassValue() != null))
                .orElse("missing"))))),
          groovy(
            """
              @interface Example {
                  Class type()
              }

              @Example(type = String)
              class Test {
              }
              """,
            """
              @interface Example {
                  Class type()
              }

              /*~~(CLASS_LITERAL:field=false:class=true)~~>*/@Example(type = String)
              class Test {
              }
              """
          )
        );
    }

    @ExpectedToFail("GroovyParserVisitor#visitPropertyExpression hard-codes fieldType=null on property-access references, so Flag.Enum is unavailable")
    @Test
    void enumConstant() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("e")
                .map(v -> v.getKind() + ":isEnum=" + v.isEnumConstant("E", "ONE"))
                .orElse("missing"))))),
          groovy(
            """
              enum E {
                  ONE, TWO
              }

              @interface Example {
                  E e()
              }

              @Example(e = E.ONE)
              class Test {
              }
              """,
            """
              enum E {
                  ONE, TWO
              }

              @interface Example {
                  E e()
              }

              /*~~(ENUM_CONSTANT:isEnum=true)~~>*/@Example(e = E.ONE)
              class Test {
              }
              """
          )
        );
    }

    @ExpectedToFail("GroovyTypeMapping builds no JavaType.Annotation element values, so the compiler's constant fold is unavailable")
    @Test
    void constantReferenceFolds() {
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

              /*~~(CONSTANT_REFERENCE:n)~~>*/@Example(name = Constants.NAME)
              class Test {
              }
              """
          )
        );
    }
}
