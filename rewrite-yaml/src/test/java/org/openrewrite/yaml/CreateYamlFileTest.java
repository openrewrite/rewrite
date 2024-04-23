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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.MockHttpSender;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;

import static org.openrewrite.yaml.Assertions.yaml;

class CreateYamlFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedFile() {
        String fileContents = """
          # This is a comment
          x:
            y: z
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            fileContents,
            null,
            null
          )),
          yaml(
            null,
            fileContents,
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
            "after: true",
            null,
            true
          )),
          yaml(
            "before: true",
            "after: true",
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            null,
            null,
            false
          )),
          yaml(
            """
              foo: bar
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
            null,
            null,
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
            null,
            null,
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

    @Test
    void shouldDownloadFileContents() {
        @Language("yml")
        String yamlContent = """
          # This is a comment
          foo: x
          bar:
            z: y
          """;
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(e -> e.printStackTrace());
        HttpSenderExecutionContextView.view(ctx).setLargeFileHttpSender(new MockHttpSender(() ->
          new ByteArrayInputStream(yamlContent.getBytes())));
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            null,
            "http://fake.url/test.yaml",
            true)
          ),
          yaml(
            null,
            yamlContent,
            spec -> spec.path("test/test.yaml")
          )
        );
    }

    @Test
    void shouldUseFileContentsWhenContentsAndContentsUrlNotNull() {
        @Language("yml")
        String fileContents = """
          # This is a comment
          x:
            y: z
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateYamlFile(
            "test/test.yaml",
            fileContents,
            "http://foo.bar/baz.yaml",
            true)
          ),
          yaml(
            null,
            fileContents,
            spec -> spec.path("test/test.yaml")
          )
        );
    }
}
