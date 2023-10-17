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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class CreateYamlFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            null
          )),
          yaml(
            null,
            "",
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            true
          )),
          yaml(
            "",
            "",
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            false
          )),
          yaml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <test/>
              """,
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            null
          )),
          yaml(
            "",
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test-file-2.yaml",
            true
          )),
          yaml(
            "",
            spec -> spec.path("test/test-file-1.yaml")
          ),
          yaml(
            null,
            "",
            spec -> spec.path("test/test-file-2.yaml")
          )
        );
    }
}
