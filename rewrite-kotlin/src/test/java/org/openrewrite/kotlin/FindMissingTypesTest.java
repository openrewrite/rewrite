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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class FindMissingTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindMissingTypes(false))
          .parser(JavaParser.fromJavaVersion())
          .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void kotlinClassReference() {
        rewriteRun(
          spec -> spec.parser(
            KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api")),
          kotlin(
            """
              package com.some.other

              class EventPublisher {
              }
              """
          ),
          kotlin(
            """
              package com.some.card

              import com.some.other.EventPublisher
              import jakarta.persistence.EntityListeners

              @EntityListeners(EventPublisher::class)
              data class Card(
              )
              """
          )
        );
    }

    @Test
    void invalidKotlinClassReference() {
        rewriteRun(
          spec -> spec.parser(
            KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api")),
          kotlin(
            """
              package com.some.card

              import jakarta.persistence.EntityListeners

              @EntityListeners(EventPublisher::class)
              data class Card(
              )
              """,
            """
              package com.some.card

              import jakarta.persistence.EntityListeners

              @EntityListeners(/*~~(MemberReference Parameterized type is missing or malformed)~~>*//*~~(Identifier type is missing or malformed)~~>*/EventPublisher::class)
              data class Card(
              )
              """
          )
        );
    }
}
