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
package org.openrewrite.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeTextTest {

    @Test
    void codeOutsideCodeSpans() {
        assertThat(RecipeText.codeOutsideCodeSpans(
          "Replaces org.junit.Test with @Test from org.junit.jupiter.api, and calls Assert#assertEquals(). " +
          "Keeps @Nested.Inner and org.example.Outer$Inner#run."
        )).containsExactly("org.junit.Test", "@Test", "Assert#assertEquals()", "@Nested.Inner",
          "org.example.Outer$Inner#run");
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Replaces `org.junit.Test` with `@Test` and calls `Assert#assertEquals`.",
      "See [the guide](https://docs.spring.io/spring-boot/org.springframework.boot.SpringApplication.html).",
      "See <https://example.com/org.example.Foo> and https://example.com/org/example/Foo#bar.",
      "Upgrade to Spring Boot 3.x, e.g. Java 17. Uses application.properties and pom.xml.",
      "Mail user@Example.com, use {@link Foo} and write C# or F#.",
      """
        Changes this:
        ```java
        @Deprecated
        org.junit.Test
        ```
        """
    })
    void noCodeOutsideCodeSpans(String markdown) {
        assertThat(RecipeText.codeOutsideCodeSpans(markdown)).isEmpty();
    }

    @Test
    void titleCaseWords() {
        assertThat(RecipeText.titleCaseWords("Migrate From JUnit 4 To JUnit 5 And Remove The Runner"))
          .containsExactly("From", "To", "And", "The");
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Migrate to Spring Boot 3.0",
      "To be removed",
      "Use `In` from `org.example`",
      "Opt-In to preview features"
    })
    void sentenceCase(String displayName) {
        assertThat(RecipeText.titleCaseWords(displayName)).isEmpty();
    }
}
