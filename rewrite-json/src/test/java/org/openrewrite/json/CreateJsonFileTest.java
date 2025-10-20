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
package org.openrewrite.json;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.remote.RemoteArtifactCache;
import org.openrewrite.remote.RemoteExecutionContextView;
import org.openrewrite.test.MockHttpSender;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import static org.openrewrite.json.Assertions.json;

class CreateJsonFileTest implements RewriteTest {
    @DocumentExample
    @Test
    void hasCreatedFile() {
        @Language("json")
        String fileContents = """
          {
            "x": {
              "y": "z"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateJsonFile(
            "test/test.json",
            fileContents,
            null,
            null
          )),
          json(
            doesNotExist(),
            fileContents,
            spec -> spec.path("test/test.json")
          )
        );
    }

    @Test
    void hasOverwrittenFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateJsonFile(
            "test/test.json",
            "{ \"after\": true }",
            null,
            true
          )),
          json(
            "{ \"before\": true }",
            "{ \"after\": true }",
            spec -> spec.path("test/test.json")
          )
        );
    }

    @NullSource
    @ParameterizedTest
    @ValueSource(booleans = {false})
    void shouldNotChangeExistingFile_whenOverwriteExistingFalseOrNull(Boolean overwriteExisting) {
        rewriteRun(
          spec -> spec.recipe(new CreateJsonFile(
            "test/test.json",
            null,
            null,
            overwriteExisting
          )),
          json(
            //language=json
            """
              {
                "foo": "bar"
              }
              """,
            spec -> spec.path("test/test.json")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateJsonFile(
            "test/test-file-2.json",
            "",
            null,
            true
          )),
          json(
            "",
            spec -> spec.path("test/test-file-1.json")
          ),
          json(
            doesNotExist(),
            "",
            spec -> spec.path("test/test-file-2.json")
          )
        );
    }

    @Test
    void shouldDownloadFileContents() {
        @Language("json")
        String jsonContent = """
          {
            "foo": "x",
            "bar": {
              "z": "y"
            }
          }
          """;
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MockHttpSender httpSender = new MockHttpSender(() -> new ByteArrayInputStream(jsonContent.getBytes()));
        HttpSenderExecutionContextView.view(ctx)
          .setHttpSender(httpSender)
          .setLargeFileHttpSender(httpSender);
        RemoteExecutionContextView.view(ctx).setArtifactCache(new RemoteArtifactCache() {
            @Override
            public @Nullable Path get(URI uri) {
                return null;
            }

            @Override
            public @Nullable Path put(URI uri, InputStream is, Consumer<Throwable> onError) {
                try {
                    var file = File.createTempFile("rewrite", "json");
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return file.toPath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        rewriteRun(
          spec -> spec
            .executionContext(ctx)
            .recipeExecutionContext(ctx)
            .recipe(new CreateJsonFile(
              "test/test.json",
              null,
              "http://fake.url/test.json",
              true
            )),
          json(
            doesNotExist(),
            jsonContent,
            spec -> spec.path("test/test.json")
          )
        );
    }

    @Test
    void shouldUseFileContentsWhenContentsAndContentsUrlNotNull() {
        @Language("json")
        String fileContents = """
          {
            "x": {
              "y": "z"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateJsonFile(
            "test/test.json",
            fileContents,
            "http://foo.bar/baz.json",
            true
          )),
          json(
            doesNotExist(),
            fileContents,
            spec -> spec.path("test/test.json")
          )
        );
    }

    @Test
    void shouldCreateJsonFromYamlRecipe() {
        //language=yaml
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.CreateJsonDeclarative
            displayName: Create json file
            description: Create json file.
            recipeList:
              - org.openrewrite.json.CreateJsonFile:
                  relativeFileName: created.json
                  overwriteExisting: false
                  fileContents: |
                    {
                      "content": "yes"
                    }
            """, "org.openrewrite.CreateJsonDeclarative"),
          json(
            //language=json
            """
              {
                "foo": "bar"
              }
              """,
            spec -> spec.path("somefile.json")
          ),
          json(
            doesNotExist(),
            """
              {
                "content": "yes"
              }
              """,
            spec -> spec.path("created.json")
          )
        );
    }

    @Test
    void shouldCreateJsonFromYamlRecipeWithPrecondition() {
        //language=yaml
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.CreateJsonPrecondition
            displayName: Create json file with precondition
            description: Create json file with a precondition.
            preconditions:
              - org.openrewrite.FindSourceFiles:
                  filePattern: "**/precondition.json"
            recipeList:
              - org.openrewrite.json.CreateJsonFile:
                  relativeFileName: created.json
                  overwriteExisting: false
                  fileContents: |
                    {
                      "content": "yes"
                    }
            """, "org.openrewrite.CreateJsonPrecondition"),
          json(
            //language=json
            """
              {
                "foo": "bar"
              }
              """,
            spec -> spec.path("precondition.json")
          ),
          json(
            doesNotExist(),
            """
              {
                "content": "yes"
              }
              """,
            spec -> spec.path("created.json")
          )
        );
    }
}
