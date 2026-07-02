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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

/**
 * Pins the {@link org.openrewrite.java.trait.AttributeValue} behavior on Kotlin sources:
 * {@code A::class} class references, and the degradation of Kotlin collection literals
 * ({@code K.ListLiteral} is not a {@code J.NewArray}).
 */
class AttributeValueTraitTest implements RewriteTest {

    @Test
    void classReference() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("clazz")
                .map(v -> {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(v.getClassValue());
                    return v.getKind() + ":" + (fq != null ? fq.getFullyQualifiedName() : "null");
                })
                .orElse("missing"))))),
          kotlin(
            """
              import kotlin.reflect.KClass

              annotation class Example(val clazz: KClass<*>)

              @Example(clazz = String::class)
              class Test
              """,
            """
              import kotlin.reflect.KClass

              annotation class Example(val clazz: KClass<*>)

              /*~~(CLASS_LITERAL:kotlin.String)~~>*/@Example(clazz = String::class)
              class Test
              """
          )
        );
    }

    @Test
    void collectionLiteralIsAnOpaqueExpression() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new Annotated.Matcher("@Example")
            .asVisitor(a -> SearchResult.found(a.getTree(),
              a.getAttributeValue("tags")
                .map(v -> v.getKind() + ":elements=" + v.getElements().size())
                .orElse("missing"))))),
          kotlin(
            """
              annotation class Example(val tags: Array<String>)

              @Example(tags = ["a", "b"])
              class Test
              """,
            """
              annotation class Example(val tags: Array<String>)

              /*~~(EXPRESSION:elements=1)~~>*/@Example(tags = ["a", "b"])
              class Test
              """
          )
        );
    }
}
