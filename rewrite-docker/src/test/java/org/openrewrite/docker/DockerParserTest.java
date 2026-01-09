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
package org.openrewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class DockerParserTest implements RewriteTest {

    @Test
    void simpleFrom() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getDigest()).isNull();
                assertThat(from.getAs()).isNull();
            })
          )
        );
    }

    @Test
    void fromWithPlatform() {
        rewriteRun(
          Assertions.docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(from.getFlags()).hasSize(1);
                assertThat(from.getFlags().getFirst().getName()).isEqualTo("platform");
                assertThat(((Docker.PlainText) from.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("linux/amd64");
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
            })
          )
        );
    }

    @Test
    void fromWithAs() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04 AS base
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getAs()).isNotNull();
                assertThat(((Docker.PlainText) from.getAs().getName().getContents().getFirst()).getText()).isEqualTo("base");
            })
          )
        );
    }

    @Test
    void simpleRun() {
        rewriteRun(
          Assertions.docker(
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
          Assertions.docker(
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
          Assertions.docker(
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
    void multipleInstructions() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void lowercaseInstructions() {
        rewriteRun(
          Assertions.docker(
            """
              from ubuntu:20.04
              run apt-get update
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getStages().getFirst().getFrom().getKeyword()).isEqualTo("from"))
          )
        );
    }

    @Test
    void mixedCaseInstructions() {
        rewriteRun(
          Assertions.docker(
            """
              From ubuntu:20.04 as builder
              Run apt-get update
              """
          )
        );
    }

    @Test
    void multiStageFrom() {
        rewriteRun(
          Assertions.docker(
            """
              FROM golang:1.20 AS builder
              RUN go build -o app .
              
              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getStages())
              .satisfiesExactly(
                golang -> assertThat(((Docker.PlainText) golang.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("golang"),
                alpine -> assertThat(((Docker.PlainText) alpine.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("alpine")
              ))
          )
        );
    }

    @Test
    void runWithLineContinuation() {
        rewriteRun(
          Assertions.docker(
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
          Assertions.docker(
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
          Assertions.docker(
            """
              FROM ubuntu:20.04
              RUN --network=none --mount=type=cache,target=/cache apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Run run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getFlags()).hasSize(2);
                assertThat(run.getFlags().get(0).getName()).isEqualTo("network");
                assertThat(((Docker.PlainText) run.getFlags().get(0).getValue().getContents().getFirst()).getText()).contains("none");
                assertThat(run.getFlags().get(1).getName()).isEqualTo("mount");
                assertThat(((Docker.PlainText) run.getFlags().get(1).getValue().getContents().getFirst()).getText()).contains("type=cache,target=/cache");
            })
          )
        );
    }

    @Test
    void cmdShellForm() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              CMD nginx -g daemon off;
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Cmd cmd = (Docker.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) cmd.getCommandLine().getForm();
                assertThat(shellForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void cmdExecForm() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              CMD ["nginx", "-g", "daemon off;"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Cmd cmd = (Docker.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Docker.ExecForm.class);
                Docker.ExecForm execForm = (Docker.ExecForm) cmd.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(3);
            })
          )
        );
    }

    @Test
    void entrypointExecForm() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ENTRYPOINT ["./app"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Entrypoint entrypoint = (Docker.Entrypoint) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(entrypoint.getCommandLine().getForm()).isInstanceOf(Docker.ExecForm.class);
                Docker.ExecForm execForm = (Docker.ExecForm) entrypoint.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void entrypointShellForm() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ENTRYPOINT /bin/sh -c 'echo hello'
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Entrypoint entrypoint = (Docker.Entrypoint) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(entrypoint.getCommandLine().getForm()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) entrypoint.getCommandLine().getForm();
                assertThat(shellForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void envSingleLine() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0
              ENV PATH=/usr/local/bin:$PATH
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Env env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(((Docker.PlainText) env1.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.PlainText) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envMultiplePairs() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0 NPM_VERSION=9.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Env env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs()).hasSize(2);
                assertThat(((Docker.PlainText) env.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.PlainText) env.getPairs().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
                assertThat(((Docker.PlainText) env.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("NPM_VERSION");
                assertThat(((Docker.PlainText) env.getPairs().get(1).getValue().getContents().getFirst()).getText()).isEqualTo("9.0.0");
            })
          )
        );
    }

    @Test
    void envOldStyleSpaceSeparated() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION 18.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Env env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(((Docker.PlainText) env1.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.PlainText) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void labelSinglePair() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(((Docker.PlainText) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.PlainText) label.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void labelMultiplePairs() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0 app=myapp
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(2);
                assertThat(((Docker.PlainText) label.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.PlainText) label.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("app");
            })
          )
        );
    }

    @Test
    void labelWithQuotedValues() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              LABEL description="My application" version="1.0.0"
              """
          )
        );
    }

    @Test
    void simpleArg() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) arg.getName().getContents().getFirst()).getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNotNull();
                assertThat(((Docker.PlainText) arg.getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void argWithoutValue() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ARG VERSION
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) arg.getName().getContents().getFirst()).getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNull();
            })
          )
        );
    }

    @Test
    void argInstructions() {
        rewriteRun(
          Assertions.docker(
            """
              ARG BASE_IMAGE=ubuntu:20.04
              FROM ${BASE_IMAGE}
              ARG VERSION
              """
          )
        );
    }

    @Test
    void workdirInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              WORKDIR /app
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Workdir workdir = (Docker.Workdir) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) workdir.getPath().getContents().getFirst()).getText()).isEqualTo("/app");
            })
          )
        );
    }

    @Test
    void userInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              USER nobody
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.User user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) user.getUser().getContents().getFirst()).getText()).isEqualTo("nobody");
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    @Test
    void userWithGroup() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              USER app:group
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.User user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) user.getUser().getContents().getFirst()).getText()).isEqualTo("app");
                assertThat(((Docker.PlainText) user.getGroup().getContents().getFirst()).getText()).isEqualTo("group");
            })
          )
        );
    }

    @Test
    void stopsignalInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              STOPSIGNAL SIGTERM
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Stopsignal stopsignal = (Docker.Stopsignal) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) stopsignal.getSignal().getContents().getFirst()).getText()).isEqualTo("SIGTERM");
            })
          )
        );
    }

    @Test
    void maintainerInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              MAINTAINER John Doe <john@example.com>
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Maintainer maintainer = (Docker.Maintainer) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(((Docker.PlainText) maintainer.getText().getContents().getFirst()).getText()).isEqualTo("John Doe <john@example.com>");
            })
          )
        );
    }

    @Test
    void simpleCopy() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Copy copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getSources()).hasSize(1);
                assertThat(((Docker.PlainText) copy.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.jar");
                assertThat(((Docker.PlainText) copy.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void addInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ADD app.tar.gz /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Add add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(add.getSources()).hasSize(1);
                assertThat(((Docker.PlainText) add.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.tar.gz");
                assertThat(((Docker.PlainText) add.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void exposeInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8080
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(1);
                assertThat(((Docker.PlainText) expose.getPorts().getFirst().getContents().getFirst()).getText()).isEqualTo("8080");
            })
          )
        );
    }

    @Test
    void exposeMultiplePorts() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8080 8443
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(2);
                assertThat(((Docker.PlainText) expose.getPorts().get(0).getContents().getFirst()).getText()).isEqualTo("8080");
                assertThat(((Docker.PlainText) expose.getPorts().get(1).getContents().getFirst()).getText()).isEqualTo("8443");
            })
          )
        );
    }

    @Test
    void volumeWithJsonArray() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              VOLUME ["/data", "/logs"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Volume volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isTrue();
            })
          )
        );
    }

    @Test
    void volumeWithPathList() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              VOLUME /data /logs
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Volume volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isFalse();
                assertThat(((Docker.PlainText) volume.getValues().get(0).getContents().getFirst()).getText()).isEqualTo("/data");
                assertThat(((Docker.PlainText) volume.getValues().get(1).getContents().getFirst()).getText()).isEqualTo("/logs");
            })
          )
        );
    }

    @Test
    void shellInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              SHELL ["/bin/bash", "-c"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Shell shell = (Docker.Shell) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(shell.getArguments()).hasSize(2);
            })
          )
        );
    }

    @Test
    void onbuildInstruction() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ONBUILD RUN apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Onbuild onbuild = (Docker.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Docker.Run.class);
            })
          )
        );
    }

    @Test
    void onbuildWithCopy() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              ONBUILD COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Onbuild onbuild = (Docker.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Docker.Copy.class);
            })
          )
        );
    }

    @Test
    void healthcheckNone() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK NONE
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Healthcheck healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isTrue();
            })
          )
        );
    }

    @Test
    void healthcheckWithCmd() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Healthcheck healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isFalse();
                assertThat(healthcheck.getCmd()).isNotNull();
            })
          )
        );
    }

    @Test
    void copyInstructions() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              COPY --chown=app:app app.jar /app/
              COPY --from=builder /build/output /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Copy copy1 = (Docker.Copy) instructions.getFirst();
                assertThat(copy1.getFlags()).hasSize(1);
                assertThat(copy1.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.PlainText) copy1.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("app:app");

                Docker.Copy copy2 = (Docker.Copy) instructions.get(1);
                assertThat(copy2.getFlags()).hasSize(1);
                assertThat(copy2.getFlags().getFirst().getName()).isEqualTo("from");
                assertThat(((Docker.PlainText) copy2.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("builder");
            })
          )
        );
    }

    @Test
    void complexExpression() {
        rewriteRun(
          Assertions.docker(
            """
              ARG VERSION=25
              FROM $REGISTRY/image:${VERSION}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getLast().getFrom();

                // Check imageName contents
                List<Docker.ArgumentContent> imageNameContents = from.getImageName().getContents();
                assertThat(imageNameContents).hasSize(2);
                assertThat(imageNameContents.getFirst()).extracting(arg -> ((Docker.EnvironmentVariable) arg).getName()).isEqualTo("REGISTRY");
                assertThat(imageNameContents.get(1)).extracting(arg -> ((Docker.PlainText) arg).getText()).isEqualTo("/image");

                // Check tag contents
                assertThat(from.getTag()).isNotNull();
                List<Docker.ArgumentContent> tagContents = from.getTag().getContents();
                assertThat(tagContents).hasSize(2);
                assertThat(tagContents.getFirst()).extracting(arg -> ((Docker.EnvironmentVariable) arg).getName()).isEqualTo("VERSION");
                assertThat(tagContents.get(1)).extracting(arg -> ((Docker.PlainText) arg).getText()).isEqualTo("-suffix");

                // Check no digest
                assertThat(from.getDigest()).isNull();
            })
          )
        );
    }

    @Test
    void comprehensiveDockerfile() {
        rewriteRun(
          Assertions.docker(
            """
              # syntax=docker/dockerfile:1
              
              # Build stage
              FROM golang:1.20 AS builder
              WORKDIR /build
              COPY go.mod go.sum ./
              RUN go mod download
              COPY . .
              RUN CGO_ENABLED=0 go build -o app
              
              # Runtime stage
              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              WORKDIR /app
              COPY --from=builder /build/app .
              EXPOSE 8080
              USER nobody
              ENTRYPOINT ["./app"]
              """
          )
        );
    }

    @Test
    void runWithHeredoc() {
        rewriteRun(
          Assertions.docker(
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
          Assertions.docker(
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
    void copyWithHeredoc() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:20.04
              COPY <<EOF /app/config.txt
              # Configuration file
              setting1=value1
              setting2=value2
              EOF
              """
          )
        );
    }
}
