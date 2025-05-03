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