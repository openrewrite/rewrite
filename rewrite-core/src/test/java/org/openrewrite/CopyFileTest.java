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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class CopyFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void copyToRelativeToUnixFileName() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application*.yml", "../resources", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/renameMe/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec
              .path("src/main/resources/application.yml")
              .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Path.of("src/main/resources/application.yml")))
          )
        );
    }

    @Test
    void copyRelativeToUnixFolderName() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile("src/main/renameMe", null, "../resources", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/renameMe/nested/deeply/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/resources/nested/deeply/application.yml")
          )
        );
    }

    @Test
    void copyFilesToUnixSubDirectory() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application*.yml", "profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/resources/profiles/application.yml")
          )
        );
    }

    @Test
    void copyFolderToUnixSubDirectory() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile("src/main", null, "nested", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/nested/resources/application.yml")
          )
        );
    }

    @Test
    void copyFilesToExactUnixPath() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application*.yml", "/profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("profiles/application.yml")
          )
        );
    }

    @Test
    void originalIsPreserved() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application.yml", "../profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/profiles/application.yml")
          )
        );
    }

    @Test
    void ignoreNonMatchingFiles() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/renameMe/application*.yml", "../resources", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/renameMe/application.yaml") // extension is wrong
          ),
          text(
            "hello: world",
            spec -> spec.path("src/main/doNotRenameMe/application.yml") // folder name is wrong
          )
        );
    }

    @Test
    void ignoreNonMatchingFolders() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile("src/main/renameMe", null, "../profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/.renameMe/application.yaml")
          ),
          text(
            "hello: world",
            spec -> spec.path("src/main/doNotRenameMe/application.yml")
          )
        );
    }

    @Test
    void rootFiles() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "*.yml", "src/main/resources", null)),
          text(
            "hello: world",
            spec -> spec.path("application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path(PathUtils.separatorsToSystem("src/main/resources/application.yml"))
          )
        );
    }

    @Test
    void copyToWindowsPath() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "src/**/application*.yml", "..\\profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src\\main\\renameMe\\application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src\\main\\profiles\\application.yml")
          )
        );
    }

    @Test
    void copyWithDestinationFilename() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application.yml", ".", "test.yml")),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/resources/test.yml")
          )
        );
    }

    @Test
    void copyWithDestinationFilenameToOtherFolder() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application.yml", "../profiles", "test.yml")),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/profiles/test.yml")
          )
        );
    }

    @Test
    void destinationFilenameRejectedWithFolder() {
        assertThat(new CopyFile("src/main/resources", null, "../profiles", "test.yml")
          .validate()
          .isInvalid()).isTrue();
    }

    @Test
    void copyMultipleFiles() {
        rewriteRun(
          spec -> spec.recipe(new CopyFile(null, "**/application*.yml", "../profiles", null)),
          text(
            "hello: world",
            spec -> spec.path("src/main/resources/application.yml")
          ),
          text(
            "goodbye: world",
            spec -> spec.path("src/main/resources/application-dev.yml")
          ),
          text(
            null,
            "hello: world",
            spec -> spec.path("src/main/profiles/application.yml")
          ),
          text(
            null,
            "goodbye: world",
            spec -> spec.path("src/main/profiles/application-dev.yml")
          )
        );
    }
}
