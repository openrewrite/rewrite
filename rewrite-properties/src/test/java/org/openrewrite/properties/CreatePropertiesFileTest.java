/*
 * Copyright 2023 the original author or authors.
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

class CreatePropertiesFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedPropertiesFile() {
        rewriteRun(
          spec -> spec.recipe(new CreatePropertiesFile(
            "test/test.properties",
            """
              # This is a comment
              x.y=z
              """,
            null
          )),
          properties(
            null,
            """
              # This is a comment
              x.y=z
              """,
            spec -> spec.path("test/test.properties")
          )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        rewriteRun(
          spec -> spec.recipe(new CreatePropertiesFile(
            "test/test.properties",
            "after=true",
            true
          )).cycles(1).expectedCyclesThatMakeChanges(1),
          properties(
            "test.property=test",
            "after=true",
            spec -> spec.path("test/test.properties")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreatePropertiesFile(
            "test/test.properties",
            "a.property=value",
            false
          )),
          properties(
            "test.property=test",
            spec -> spec.path("test/test.properties")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
          spec -> spec.recipe(new CreatePropertiesFile(
            "test/test.properties",
            null,
            null
          )),
          properties(
            "test.property=test",
            spec -> spec.path("test/test.properties")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreatePropertiesFile(
            "test/test-file-2.properties",
            null,
            true
          )),
          properties(
            "test.property=test",
            spec -> spec.path("test/test-file-1.properties")
          ),
          properties(
            null,
            "",
            spec -> spec.path("test/test-file-2.properties")
          )
        );
    }
}
