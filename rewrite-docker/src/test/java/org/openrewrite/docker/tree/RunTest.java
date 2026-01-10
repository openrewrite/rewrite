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
package org.openrewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class RunTest implements RewriteTest {

    @Test
    void simpleRun() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommandLine().getForm();
                assertThat(shellForm.getArguments())
                  .singleElement()
                  .extracting(arg -> ((Docker.PlainText) arg.getContents().getFirst()).getText())
                  .isEqualTo("apt-get update");
            })
          )
        );
    }

    @Test
    void runExecForm() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN ["apt-get", "update"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Docker.ExecForm.class);
                Docker.ExecForm execForm = (Docker.ExecForm) run.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(2)
                  .satisfiesExactly(
                    arg -> assertThat(((Docker.QuotedString) arg.getContents().getFirst()).getValue()).isEqualTo("apt-get"),
                    arg -> assertThat(((Docker.QuotedString) arg.getContents().getFirst()).getValue()).isEqualTo("update")
                  );
            })
          )
        );
    }

    @Test
    void runWithEnvironmentVariable() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN CGO_ENABLED=0 go build -o app
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getLast().getInstructions().getFirst();
                assertThat(run.getFlags()).isNull();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommandLine().getForm();
                assertThat(shellForm.getArguments())
                  .singleElement()
                  .extracting(arg -> ((Docker.PlainText) arg.getContents().getFirst()).getText())
                  .isEqualTo("CGO_ENABLED=0 go build -o app");
            })
          )
        );
    }

    @Test
    void runWithLineContinuation() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update && \\
                  apt-get install -y curl && \\
                  rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void runWithSimpleFlag() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN --network=none apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getFlags()).hasSize(1);
                assertThat(run.getFlags().getFirst().getName()).isEqualTo("network");
                assertThat(((Docker.PlainText) run.getFlags().getFirst().getValue().getContents().getFirst()).getText()).contains("none");
            })
          )
        );
    }

    @Test
    void runWithMultipleFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN --network=none --mount=type=cache,target=/cache apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getFlags()).hasSize(2);
                assertThat(run.getFlags().get(0).getName()).isEqualTo("network");
                assertThat(((Docker.PlainText) run.getFlags().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("none");
                assertThat(run.getFlags().get(1).getName()).isEqualTo("mount");
                // Flag value is parsed as multiple elements: type, =, cache,target, =, /cache
                Docker.Argument mountValue = run.getFlags().get(1).getValue();
                assertThat(mountValue.getContents()).hasSize(5);
                assertThat(((Docker.PlainText) mountValue.getContents().get(0)).getText()).isEqualTo("type");
            })
          )
        );
    }

    @Test
    void runWithHeredoc() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
              apt-get update
              apt-get install -y curl
              EOF
              """
          )
        );
    }

    @Test
    void runWithHeredocDash() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<-FILE_END
              echo "Hello World"
              echo "Another line"
              FILE_END
              """
          )
        );
    }

    @Test
    void runWithChownFlag() {
        // Test chown -R with space before next argument
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN chown -R user:user /home
              """
          )
        );
    }

    @Test
    void runWithGitClone() {
        // Test git clone with URL
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN git clone https://github.com/example/repo.git
              """
          )
        );
    }

    @Test
    void runWithShellKeyword() {
        // Test useradd with --shell flag (shell is a Docker keyword)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN useradd --shell /bin/false user
              """
          )
        );
    }

    @Test
    void runWithLineContinuationAndShellKeyword() {
        // Test useradd with --shell flag across line continuations
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN useradd --no-log-init \\
                  --uid=1654 \\
                  --shell /bin/false \\
                  --create-home \\
                  app
              """
          )
        );
    }
}
