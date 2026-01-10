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
                assertThat(run.getCommand()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommand();
                assertThat(shellForm.getArgument().getText()).isEqualTo("apt-get update");
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
                assertThat(run.getCommand()).isInstanceOf(Docker.ExecForm.class);
                Docker.ExecForm execForm = (Docker.ExecForm) run.getCommand();
                assertThat(execForm.getArguments()).hasSize(2)
                  .satisfiesExactly(
                    arg -> assertThat(arg.getText()).isEqualTo("apt-get"),
                    arg -> assertThat(arg.getText()).isEqualTo("update")
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
                assertThat(run.getCommand()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommand();
                assertThat(shellForm.getArgument().getText()).isEqualTo("CGO_ENABLED=0 go build -o app");
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
    void runWithLineContinuationAndConditional() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY app.jar /app/
              RUN if [ -d /app ]; then \\
                  echo "App directory exists"; \\
              else \\
                  mkdir -p /app; \\
              fi
              """,
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().getLast();
                var form = (Docker.CommandForm.ShellForm) run.getCommand();
                assertThat(form.getArgument().getText()).isEqualTo("""
                  if [ -d /app ]; then \\
                      echo "App directory exists"; \\
                  else \\
                      mkdir -p /app; \\
                  fi\
                  """);
            })
          )
        );
    }

    @Test
    void runWithConditional() {
        rewriteRun(
          docker(
            """
              ARG VERSION
              FROM golang AS backend
              RUN if [[ -z "$VERSION" ]] ; then make heedy ; else make heedy VERSION=$VERSION ; fi
              """,
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().getLast();
                var form = (Docker.CommandForm.ShellForm) run.getCommand();
                assertThat(form.getArgument().getText()).isEqualTo("if [[ -z \"$VERSION\" ]] ; then make heedy ; else make heedy VERSION=$VERSION ; fi");
            })
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
                assertThat(((Docker.Literal) run.getFlags().getFirst().getValue().getContents().getFirst()).getText()).contains("none");
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
                assertThat(((Docker.Literal) run.getFlags().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("none");
                assertThat(run.getFlags().get(1).getName()).isEqualTo("mount");
                // Flag value is parsed as multiple elements: type, =, cache,target, =, /cache
                Docker.Argument mountValue = run.getFlags().get(1).getValue();
                assertThat(mountValue.getContents()).hasSize(5);
                assertThat(((Docker.Literal) mountValue.getContents().get(0)).getText()).isEqualTo("type");
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

    @Test
    void runWithShellTestBrackets() {
        // Test shell test expressions with [ and ] brackets
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN if [ ! -f /app/config.txt ]; then echo "File not found"; fi
              """
          )
        );
    }

    @Test
    void runWithComplexShellTest() {
        // Test complex shell expressions with brackets and multiple conditions
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN [ -f packages.txt ] && xargs apt-get install -y < packages.txt || echo "No packages"
              """
          )
        );
    }

    @Test
    void runWithFindExec() {
        // Test find -exec with escaped semicolon \;
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN find / -perm /6000 -type f -exec chmod a-s {} \\; || true
              """
          )
        );
    }

    @Test
    void runWithSingleQuotedRegex() {
        // Test single-quoted strings with regex special characters (backslash, parens, pipe)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN regex='^\\(root\\|app\\):' && grep $regex /etc/passwd
              """
          )
        );
    }

    @Test
    void runWithCommandSubstitution() {
        // Test $(command) command substitution
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN export GNUPGHOME="$(mktemp -d)" && echo "Using $GNUPGHOME"
              """
          )
        );
    }

    @Test
    void runWithNestedCommandSubstitution() {
        // Test nested command substitution
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN make -j$(getconf _NPROCESSORS_ONLN)
              """
          )
        );
    }

    @Test
    void runWithBacktickSubstitution() {
        // Test backtick command substitution
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN echo "Current date: `date`"
              """
          )
        );
    }

    @Test
    void runWithSpecialShellVariables() {
        // Test special shell variables like $!, $$, $?
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN myapp & pidsave=$!; sleep 10; kill $pidsave || true
              """
          )
        );
    }

    @Test
    void runWithWindowsBacktickContinuation() {
        // Test Windows-style backtick line continuation
        rewriteRun(
          docker(
            """
              # escape=`
              FROM mcr.microsoft.com/windows/servercore:ltsc2022
              RUN powershell -Command " `
                  $ErrorActionPreference = 'Stop'; `
                  Write-Host 'Hello World'"
              """
          )
        );
    }

    @Test
    void runWithWindowsMultilineString() {
        // Test Windows-style multi-line string with backtick continuation
        rewriteRun(
          docker(
            """
              # escape=`
              FROM mcr.microsoft.com/windows/servercore:ltsc2022
              RUN powershell -Command " `
                  $var = 'test'; `
                  if ($var -eq 'test') { `
                      Write-Host 'Match'; `
                  }"
              """
          )
        );
    }
}
