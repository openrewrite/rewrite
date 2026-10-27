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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseDefaultPathSensitivityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // The first test in this module to parse build logic rather than a build script, so the Gradle API has to be
        // on the parser's classpath for `@InputFile` and friends to resolve to their types.
        spec.recipe(new UseDefaultPathSensitivity())
                .parser(JavaParser.fromJavaVersion().classpath("gradle-api"));
    }

    @DocumentExample("Make a file input's cache key independent of where the file sits")
    @Test
    void addsNoneToAnUnannotatedFileInput() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.RegularFileProperty;
              import org.gradle.api.tasks.InputFile;

              public abstract class Report extends DefaultTask {
                  @InputFile
                  public abstract RegularFileProperty getTemplate();
              }
              """,
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.RegularFileProperty;
              import org.gradle.api.tasks.InputFile;
              import org.gradle.api.tasks.PathSensitive;
              import org.gradle.api.tasks.PathSensitivity;

              public abstract class Report extends DefaultTask {
                  @InputFile
                  @PathSensitive(PathSensitivity.NONE)
                  public abstract RegularFileProperty getTemplate();
              }
              """
          )
        );
    }

    @Test
    void replacesAbsoluteOnAFileInput() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.RegularFileProperty;
              import org.gradle.api.tasks.InputFile;
              import org.gradle.api.tasks.PathSensitive;
              import org.gradle.api.tasks.PathSensitivity;

              public abstract class Report extends DefaultTask {
                  @InputFile
                  @PathSensitive(PathSensitivity.ABSOLUTE)
                  public abstract RegularFileProperty getTemplate();
              }
              """,
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.RegularFileProperty;
              import org.gradle.api.tasks.InputFile;
              import org.gradle.api.tasks.PathSensitive;
              import org.gradle.api.tasks.PathSensitivity;

              public abstract class Report extends DefaultTask {
                  @InputFile
                  @PathSensitive(PathSensitivity.NONE)
                  public abstract RegularFileProperty getTemplate();
              }
              """
          )
        );
    }

    @Test
    void addsRelativeToADirectoryInput() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.DirectoryProperty;
              import org.gradle.api.tasks.InputDirectory;

              public abstract class Report extends DefaultTask {
                  @InputDirectory
                  public abstract DirectoryProperty getSources();
              }
              """,
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.DirectoryProperty;
              import org.gradle.api.tasks.InputDirectory;
              import org.gradle.api.tasks.PathSensitive;
              import org.gradle.api.tasks.PathSensitivity;

              public abstract class Report extends DefaultTask {
                  @InputDirectory
                  @PathSensitive(PathSensitivity.RELATIVE)
                  public abstract DirectoryProperty getSources();
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyRelative() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.DirectoryProperty;
              import org.gradle.api.tasks.InputDirectory;
              import org.gradle.api.tasks.PathSensitive;
              import org.gradle.api.tasks.PathSensitivity;

              public abstract class Report extends DefaultTask {
                  @InputDirectory
                  @PathSensitive(PathSensitivity.RELATIVE)
                  public abstract DirectoryProperty getSources();
              }
              """
          )
        );
    }

    /**
     * The right sensitivity for a file collection depends on whether the task cares about the names within it, which
     * the annotation alone does not say, so `@InputFiles` is reported by the catalog but not changed here.
     */
    @Test
    void noChangeToAFileCollectionInput() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.ConfigurableFileCollection;
              import org.gradle.api.tasks.InputFiles;

              public abstract class Report extends DefaultTask {
                  @InputFiles
                  public abstract ConfigurableFileCollection getExtras();
              }
              """
          )
        );
    }

    @Test
    void noChangeToAnOutput() {
        rewriteRun(
          java(
            """
              import org.gradle.api.DefaultTask;
              import org.gradle.api.file.RegularFileProperty;
              import org.gradle.api.tasks.OutputFile;

              public abstract class Report extends DefaultTask {
                  @OutputFile
                  public abstract RegularFileProperty getReport();
              }
              """
          )
        );
    }
}
