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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class MoveFileTest implements RewriteTest {


    @Test
    @DocumentExample
    void renameDirectory() {
        rewriteRun(
          spec -> spec.recipe(new MoveFile("**/application*.yml", "../resources")),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/renameMe/application.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("src/main/resources/application.yml")))
          ),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/renameMe/application-dev.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("src/main/resources/application-dev.yml")))
          )
        );
    }

    @Test
    void moveToSubDirectory() {
        rewriteRun(
          spec -> spec.recipe(new MoveFile("**/application*.yml", "profiles")),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/resources/application.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("src/main/resources/profiles/application.yml")))
          ),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/resources/application-dev.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("src/main/resources/profiles/application-dev.yml")))
          )
        );
    }

    @Test
    void moveToExactPath() {
        rewriteRun(
          spec -> spec.recipe(new MoveFile("**/application*.yml", "/profiles")),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/resources/application.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("profiles/application.yml")))
          ),
          text(
            "hello: world",
            "hello: world",
            spec ->
              spec
                .path("src/main/resources/application-dev.yml")
                .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Paths.get("profiles/application-dev.yml")))
          )
        );
    }

    @Test
    void ignoreNonMatchingFiles() {
        rewriteRun(
          spec -> spec.recipe(new MoveFile("**/renameMe/application*.yml", "../resources")),
          text(
            "hello: world",
            spec -> spec.path("src/main/renameMe/application.yaml")
          ),
          text(
            "hello: world",
            spec -> spec.path("src/main/doNotRenameMe/application.yml")
          )
        );
    }
}
