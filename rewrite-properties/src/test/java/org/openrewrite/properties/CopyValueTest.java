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
package org.openrewrite.properties;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class CopyValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void copyValueSameFile() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", null, "destination.key", null, null, null)),
          properties(
            """
              source.key=myValue
              destination.key=original
              """,
            """
              source.key=myValue
              destination.key=myValue
              """
          )
        );
    }

    @Test
    void copyValueCreatesNewKey() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", null, "destination.key", null, null, null)),
          properties(
            "source.key=myValue",
            """
              source.key=myValue
              destination.key=myValue"""
          )
        );
    }

    @Test
    void createNewKeysFalse() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", null, "destination.key", null, false, null)),
          properties(
            "source.key=myValue"
          )
        );
    }

    @Test
    void noChangeWhenValueAlreadyMatches() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", null, "destination.key", null, null, null)),
          properties(
            """
              source.key=myValue
              destination.key=myValue
              """
          )
        );
    }

    @Test
    void copyFromSpecificFile() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", "a.properties", "destination.key", null, null, null)),
          properties(
            """
              source.key=fromA
              destination.key=original
              """,
            """
              source.key=fromA
              destination.key=fromA
              """,
            spec -> spec.path("a.properties")
          ),
          properties(
            """
              source.key=fromB
              destination.key=original
              """,
            spec -> spec.path("b.properties")
          )
        );
    }

    @Test
    void copyToOtherFile() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", "a.properties", "destination.key", "b.properties", null, null)),
          properties(
            "source.key=fromA",
            spec -> spec.path("a.properties")
          ),
          properties(
            "destination.key=original",
            "destination.key=fromA",
            spec -> spec.path("b.properties")
          )
        );
    }

    @Test
    void copyToOtherFileCreatesNewKey() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", "a.properties", "destination.key", "b.properties", null, null)),
          properties(
            "source.key=fromA",
            spec -> spec.path("a.properties")
          ),
          properties(
            "other.key=something",
            """
              other.key=something
              destination.key=fromA""",
            spec -> spec.path("b.properties")
          )
        );
    }

    @Test
    void sourceKeyNotFound() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("nonexistent.key", null, "destination.key", null, null, null)),
          properties(
            """
              source.key=myValue
              destination.key=original
              """
          )
        );
    }

    @Test
    void relaxedBindingMatchesSource() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("my-source-key", null, "destination.key", null, null, null)),
          properties(
            """
              my_source_key=myValue
              destination.key=original
              """,
            """
              my_source_key=myValue
              destination.key=myValue
              """
          )
        );
    }

    @Test
    void copyValueInMultipleFiles() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("source.key", null, "destination.key", null, null, null)),
          properties(
            """
              source.key=valueA
              destination.key=original
              """,
            """
              source.key=valueA
              destination.key=valueA
              """,
            spec -> spec.path("a.properties")
          ),
          properties(
            """
              source.key=valueB
              destination.key=original
              """,
            """
              source.key=valueB
              destination.key=valueB
              """,
            spec -> spec.path("b.properties")
          )
        );
    }

    @Test
    void exactMatchingDisablesRelaxedBinding() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue("my-source-key", null, "destination.key", null, null, false)),
          properties(
            """
              my_source_key=myValue
              destination.key=original
              """
          )
        );
    }
}
