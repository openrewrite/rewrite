/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.text;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.SourceSpecs.text;

class CreateTextFileTest implements RewriteTest {

    @Test
    void hasCreatedFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo", ".github/CODEOWNERS", false)),
          text(
            null,
            "foo",
            spec -> spec.path(".github/CODEOWNERS")
          )
        );
    }

    @Test
    void hasCreatedFileWithTrailingNewline() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo\n", ".github/CODEOWNERS", false)),
          text(
            null,
            "foo\n",
            spec -> spec.path(".github/CODEOWNERS").noTrim()
          )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo", ".github/CODEOWNERS", true)),
          text(
            "hello",
            "foo",
            spec -> spec.path(".github/CODEOWNERS")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo", ".github/CODEOWNERS", false)),
          text(
            "hello",
            spec -> spec.path(".github/CODEOWNERS")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo", ".github/CODEOWNERS", null)),
          text(
            "hello",
            spec -> spec.path(".github/CODEOWNERS")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile("foo", ".github/CODEOWNERS", null)),
          text(
            "hello",
            spec -> spec.path(".github/CODEOWNERSZ")
          ),
          text(
            null,
            "foo",
            spec -> spec.path(".github/CODEOWNERS")
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-jenkins/issues/52")
    void shouldOverrideDifferentSourceFileType() {
        @Language("groovy")
        String after = """
          /*
           See the documentation for more options:
           https://github.com/jenkins-infra/pipeline-library/
          */
          buildPlugin(
            useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
            configurations: [
              [platform: 'linux', jdk: 21],
              [platform: 'windows', jdk: 17],
          ])""";
        rewriteRun(
          spec -> spec.recipe(new CreateTextFile(after, "Jenkinsfile", true)),
          groovy(
            """
              #!groovy
              buildPlugin()
              """,
            after,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }
}
