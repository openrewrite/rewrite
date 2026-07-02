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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class AttributeValueTest implements RewriteTest {

    private static org.openrewrite.Recipe describeAttribute(String annotationSignature, String attribute,
                                                            Function<AttributeValue, String> describer) {
        return RewriteTest.toRecipe(() -> new Annotated.Matcher(annotationSignature)
          .asVisitor(a -> a.getAttributeValue(attribute)
            .map(value -> (J.Annotation) SearchResult.found(a.getTree(), describer.apply(value)))
            .orElseGet(() -> SearchResult.found(a.getTree(), "missing"))));
    }

    @Test
    void literalValues() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "name",
            v -> v.getKind() + ":" + v.getConstantValue() + ":" + v.getValue(String.class))),
          java(
            """
              @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              @Example(name = "s")
              class Test {
              }
              """,
            """
              /*~~(LITERAL:s:s)~~>*/@Example(name = "s")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void negativeNumberIsLiteral() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "count",
            v -> v.getKind() + ":" + v.getConstantValue())),
          java(
            """
              @interface Example {
                  int count() default 0;
              }
              """
          ),
          java(
            """
              @Example(count = -1)
              class Test {
              }
              """,
            """
              /*~~(LITERAL:-1)~~>*/@Example(count = -1)
              class Test {
              }
              """
          )
        );
    }

    @Test
    void parenthesesAreTransparentButOldAccessorIsUnchanged() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> {
                AttributeValue value = a.getAttributeValue("name").orElseThrow(IllegalStateException::new);
                //noinspection deprecation
                return SearchResult.found(a.getTree(),
                  value.getKind() + ":" + value.getConstantValue() +
                  ":asLiteral=" + value.asLiteral().isPresent() +
                  ":oldApi=" + a.getAttribute("name").isPresent());
            }))),
          java(
            """
              @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              @Example(name = ("a"))
              class Test {
              }
              """,
            """
              /*~~(LITERAL:a:asLiteral=true:oldApi=false)~~>*/@Example(name = ("a"))
              class Test {
              }
              """
          )
        );
    }

    @Test
    void binaryExpression() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "name",
            v -> v.getKind() + ":" + v.getConstantValue())),
          java(
            """
              @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              @Example(name = "a" + "b")
              class Test {
              }
              """,
            """
              /*~~(EXPRESSION:ab)~~>*/@Example(name = "a" + "b")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void classLiterals() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@com.x.Example", "clazz", v -> {
              JavaType.FullyQualified fq = TypeUtils.asFullyQualified(v.getClassValue());
              JavaType classValue = v.getClassValue();
              return v.getKind() +
                     ":" + (fq != null ? fq.getFullyQualifiedName() : classValue == null ? "null" : classValue.getClass().getSimpleName()) +
                     ":" + v.isClassLiteral("com.x.A");
          })),
          java(
            """
              package com.x;
              public @interface Example {
                  Class<?> clazz() default Object.class;
              }
              """
          ),
          java(
            """
              package com.x;
              public class A {
              }
              """
          ),
          java(
            """
              import com.x.A;
              import com.x.Example;

              @Example(clazz = A.class)
              class Test1 {
              }

              @Example(clazz = int.class)
              class Test2 {
              }

              @Example(clazz = String[].class)
              class Test3 {
              }
              """,
            """
              import com.x.A;
              import com.x.Example;

              /*~~(CLASS_LITERAL:com.x.A:true)~~>*/@Example(clazz = A.class)
              class Test1 {
              }

              /*~~(CLASS_LITERAL:Primitive:false)~~>*/@Example(clazz = int.class)
              class Test2 {
              }

              /*~~(CLASS_LITERAL:Array:false)~~>*/@Example(clazz = String[].class)
              class Test3 {
              }
              """
          )
        );
    }

    @Test
    void classLiteralArrayAndBracelessForm() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@com.x.Example", "exclude",
            v -> v.getKind() + ":array=" + v.isArray() + ":asLiteral=" + v.asLiteral().isPresent() + ":" + v.getElements().stream()
              .map(e -> e.getKind() + "/" + e.isClassLiteral("com.x.A") + "/" + e.getName())
              .collect(Collectors.joining(",")))),
          java(
            """
              package com.x;
              public @interface Example {
                  Class<?>[] exclude() default {};
              }
              """
          ),
          java(
            """
              package com.x;
              public class A {
              }
              """
          ),
          java(
            """
              package com.x;
              public class B {
              }
              """
          ),
          java(
            """
              import com.x.A;
              import com.x.B;
              import com.x.Example;

              @Example(exclude = {A.class, B.class})
              class Test1 {
              }

              @Example(exclude = A.class)
              class Test2 {
              }

              @Example(exclude = {})
              class Test3 {
              }
              """,
            """
              import com.x.A;
              import com.x.B;
              import com.x.Example;

              /*~~(ARRAY:array=true:asLiteral=false:CLASS_LITERAL/true/exclude,CLASS_LITERAL/false/exclude)~~>*/@Example(exclude = {A.class, B.class})
              class Test1 {
              }

              /*~~(CLASS_LITERAL:array=false:asLiteral=false:CLASS_LITERAL/true/exclude)~~>*/@Example(exclude = A.class)
              class Test2 {
              }

              /*~~(ARRAY:array=true:asLiteral=true:)~~>*/@Example(exclude = {})
              class Test3 {
              }
              """
          )
        );
    }

    @Test
    void enumConstantsInAllSpellings() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@com.x.Example", "e", v -> {
              JavaType.Variable field = v.getReferencedField();
              return v.getKind() +
                     ":" + v.isEnumConstant("com.x.E", "ONE") +
                     ":" + (field != null ? TypeUtils.asFullyQualified(field.getOwner()).getFullyQualifiedName() + "." + field.getName() : "null");
          })),
          java(
            """
              package com.x;
              public @interface Example {
                  E e() default E.ONE;
              }
              """
          ),
          java(
            """
              package com.x;
              public enum E {
                  ONE, TWO
              }
              """
          ),
          java(
            """
              import com.x.E;
              import com.x.Example;

              import static com.x.E.ONE;

              @Example(e = E.ONE)
              class Test1 {
              }

              @Example(e = com.x.E.TWO)
              class Test2 {
              }

              @Example(e = ONE)
              class Test3 {
              }
              """,
            """
              import com.x.E;
              import com.x.Example;

              import static com.x.E.ONE;

              /*~~(ENUM_CONSTANT:true:com.x.E.ONE)~~>*/@Example(e = E.ONE)
              class Test1 {
              }

              /*~~(ENUM_CONSTANT:false:com.x.E.TWO)~~>*/@Example(e = com.x.E.TWO)
              class Test2 {
              }

              /*~~(ENUM_CONSTANT:true:com.x.E.ONE)~~>*/@Example(e = ONE)
              class Test3 {
              }
              """
          )
        );
    }

    @Test
    void constantReferencesInAllSpellings() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@com.x.Example", "name", v -> {
              JavaType.Variable field = v.getReferencedField();
              return v.getKind() +
                     ":" + (field != null ? TypeUtils.asFullyQualified(field.getOwner()).getFullyQualifiedName() + "." + field.getName() : "null") +
                     ":" + v.getConstantValue();
          })),
          java(
            """
              package com.x;
              public @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              package com.x;
              public class Constants {
                  public static final String NAME = "n";
              }
              """
          ),
          java(
            """
              import com.x.Constants;
              import com.x.Example;

              import static com.x.Constants.NAME;

              @Example(name = Constants.NAME)
              class Test1 {
              }

              @Example(name = NAME)
              class Test2 {
              }

              @Example(name = Test3.LOCAL)
              class Test3 {
                  static final String LOCAL = "l";
              }
              """,
            """
              import com.x.Constants;
              import com.x.Example;

              import static com.x.Constants.NAME;

              /*~~(CONSTANT_REFERENCE:com.x.Constants.NAME:n)~~>*/@Example(name = Constants.NAME)
              class Test1 {
              }

              /*~~(CONSTANT_REFERENCE:com.x.Constants.NAME:n)~~>*/@Example(name = NAME)
              class Test2 {
              }

              /*~~(CONSTANT_REFERENCE:Test3.LOCAL:l)~~>*/@Example(name = Test3.LOCAL)
              class Test3 {
                  static final String LOCAL = "l";
              }
              """
          )
        );
    }

    @Test
    void nestedAnnotation() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "bar",
            v -> v.getKind() + ":" + v.asAnnotated()
              .flatMap(nested -> nested.getDefaultAttributeValue(null))
              .map(nested -> nested.getKind() + "/" + nested.getConstantValue() + "/" + nested.getName())
              .orElse("none"))),
          java(
            """
              @interface Bar {
                  String value();
              }
              """
          ),
          java(
            """
              @interface Example {
                  Bar bar();
              }
              """
          ),
          java(
            """
              @Example(bar = @Bar("x"))
              class Test {
              }
              """,
            """
              /*~~(NESTED_ANNOTATION:LITERAL/x/value)~~>*/@Example(bar = @Bar("x"))
              class Test {
              }
              """
          )
        );
    }

    @Test
    void mixedArray() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "tags",
            v -> v.getKind() + ":" + v.getElements().stream()
              .map(e -> e.getKind() + "/" + e.getConstantValue())
              .collect(Collectors.joining(",")) +
              ":asLiteral=" + v.asLiteral().isPresent())),
          java(
            """
              @interface Example {
                  String[] tags() default {};
              }
              """
          ),
          java(
            """
              class Constants {
                  static final String NAME = "n";
              }
              """
          ),
          java(
            """
              @Example(tags = {"a", Constants.NAME})
              class Test {
              }
              """,
            """
              /*~~(ARRAY:LITERAL/a,CONSTANT_REFERENCE/n:asLiteral=false)~~>*/@Example(tags = {"a", Constants.NAME})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void positionalValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("value").map(v -> "byName=" + v.getConstantValue() + "/" + v.getName()).orElse("byName=missing") +
              ":" +
              a.getDefaultAttributeValue(null).map(v -> "byDefault=" + v.getConstantValue()).orElse("byDefault=missing"))))),
          java(
            """
              @interface Example {
                  String value();
              }
              """
          ),
          java(
            """
              @Example("x")
              class Test {
              }
              """,
            """
              /*~~(byName=x/value:byDefault=x)~~>*/@Example("x")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void absentAttributeAndEmptyParens() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              "attr=" + a.getAttributeValue("name").isPresent() +
              ":default=" + a.getDefaultAttributeValue("name").isPresent())))),
          java(
            """
              @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              @Example
              class Test1 {
              }

              @Example()
              class Test2 {
              }
              """,
            """
              /*~~(attr=false:default=false)~~>*/@Example
              class Test1 {
              }

              /*~~(attr=false:default=false)~~>*/@Example()
              class Test2 {
              }
              """
          )
        );
    }

    @Test
    void matcherMatchesOnlyTopLevelValuePositions() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AttributeValue.Matcher()
            .asVisitor((v, ctx) -> SearchResult.found(v.getTree(), v.getKind().toString())))),
          java(
            """
              @interface Example {
                  String name() default "";
                  Class<?>[] exclude() default {};
              }
              """
          ),
          java(
            """
              @Example(name = "s", exclude = {String.class})
              class Test1 {
              }

              @Example()
              class Test2 {
              }
              """,
            """
              @Example(name = /*~~(LITERAL)~~>*/"s", exclude = /*~~(ARRAY)~~>*/{String.class})
              class Test1 {
              }

              @Example()
              class Test2 {
              }
              """
          )
        );
    }

    @Test
    void elementsAreIdentityBoundToTheTree() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> {
                AttributeValue value = a.getAttributeValue("exclude").orElseThrow(IllegalStateException::new);
                List<AttributeValue> elements = value.getElements();
                J.NewArray array = (J.NewArray) value.getTree();
                assertThat(elements).hasSize(2);
                for (int i = 0; i < elements.size(); i++) {
                    assertThat(elements.get(i).getTree()).isSameAs(array.getInitializer().get(i));
                    assertThat((Object) elements.get(i).getCursor().getParentTreeCursor().getValue()).isSameAs(array);
                }
                return a.getTree();
            }))),
          java(
            """
              @interface Example {
                  Class<?>[] exclude() default {};
              }
              """
          ),
          java(
            """
              @Example(exclude = {String.class, Integer.class})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void foldsConstantsOnFieldMethodAndParameterDeclarations() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("name").map(v -> "name=" + v.getConstantValue())
                .orElseGet(() -> a.getAttributeValue("count").map(v -> "count=" + v.getConstantValue())
                  .orElse("missing")))))),
          java(
            """
              @interface Example {
                  String name() default "";
                  int count() default 0;
              }
              """
          ),
          java(
            """
              class Constants {
                  static final String NAME = "n";
              }
              """
          ),
          java(
            """
              class Test {
                  @Example(name = Constants.NAME)
                  String field;

                  private @Example(name = Constants.NAME) String afterModifier;

                  static @Example(name = Constants.NAME) final String betweenModifiers = "x";

                  @Example(name = Constants.NAME)
                  void method(@Example(count = Integer.MAX_VALUE) int param) {
                  }
              }
              """,
            """
              class Test {
                  /*~~(name=n)~~>*/@Example(name = Constants.NAME)
                  String field;

                  private /*~~(name=n)~~>*/@Example(name = Constants.NAME) String afterModifier;

                  static /*~~(name=n)~~>*/@Example(name = Constants.NAME) final String betweenModifiers = "x";

                  /*~~(name=n)~~>*/@Example(name = Constants.NAME)
                  void method(/*~~(count=2147483647)~~>*/@Example(count = Integer.MAX_VALUE) int param) {
                  }
              }
              """
          )
        );
    }

    @Test
    void foldsPositionalAndBracelessArrayValues() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getDefaultAttributeValue(null)
                .map(v -> String.valueOf(v.getConstantValue()))
                .orElseGet(() -> a.getAttributeValue("tags")
                  .map(v -> "tags=" + v.getConstantValue())
                  .orElse("missing")))))),
          java(
            """
              @interface Example {
                  String value() default "";
                  String[] tags() default {};
              }
              """
          ),
          java(
            """
              class Constants {
                  static final String NAME = "n";
              }
              """
          ),
          java(
            """
              @Example(Constants.NAME)
              class Test1 {
              }

              @Example(tags = Constants.NAME)
              class Test2 {
              }
              """,
            """
              /*~~(n)~~>*/@Example(Constants.NAME)
              class Test1 {
              }

              /*~~(tags=n)~~>*/@Example(tags = Constants.NAME)
              class Test2 {
              }
              """
          )
        );
    }

    @Test
    void repeatedAnnotationsDoNotFold() {
        rewriteRun(
          spec -> spec.recipe(describeAttribute("@Example", "name",
            v -> v.getKind() + ":" + v.getConstantValue())),
          java(
            """
              import java.lang.annotation.Repeatable;

              @Repeatable(Examples.class)
              @interface Example {
                  String name() default "";
              }
              """
          ),
          java(
            """
              @interface Examples {
                  Example[] value();
              }
              """
          ),
          java(
            """
              class Constants {
                  static final String NAME = "n";
              }
              """
          ),
          java(
            """
              @Example(name = Constants.NAME)
              @Example(name = "b")
              class Test {
              }
              """,
            """
              /*~~(CONSTANT_REFERENCE:null)~~>*/@Example(name = Constants.NAME)
              /*~~(LITERAL:b)~~>*/@Example(name = "b")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void degradesGracefullyWithoutTypeAttribution() {
        rewriteRun(
          spec -> spec
            .recipe(RewriteTest.toRecipe(() -> new AttributeValue.Matcher()
              .asVisitor((v, ctx) -> SearchResult.found(v.getTree(),
                v.getKind() +
                "/" + v.isEnumConstant("com.x.E", "ONE") +
                "/" + v.isClassLiteral("com.x.A") +
                "/" + (v.getReferencedField() != null) +
                "/" + (v.getClassValue() != null)))))
            .typeValidationOptions(TypeValidation.none()),
          java(
            """
              @Example(e = E.ONE)
              class Test1 {
              }

              @Example(clazz = A.class)
              class Test2 {
              }
              """,
            """
              @Example(e = /*~~(CONSTANT_REFERENCE/false/false/false/false)~~>*/E.ONE)
              class Test1 {
              }

              @Example(clazz = /*~~(CLASS_LITERAL/false/false/false/false)~~>*/A.class)
              class Test2 {
              }
              """
          )
        );
    }
}
