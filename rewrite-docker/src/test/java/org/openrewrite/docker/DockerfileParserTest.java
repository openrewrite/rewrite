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
package org.openrewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.docker.tree.Dockerfile;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.dockerfile;

class DockerfileParserTest implements RewriteTest {

    @Test
    void simpleFrom() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Dockerfile.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Dockerfile.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getDigest()).isNull();
                assertThat(from.getAs()).isNull();
            })
          )
        );
    }

    @Test
    void fromWithPlatform() {
        rewriteRun(
          dockerfile(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.From from = doc.getStages().getFirst().getFrom();
                assertThat(from.getFlags()).hasSize(1);
                assertThat(from.getFlags().getFirst().getName()).isEqualTo("platform");
                assertThat(((Dockerfile.PlainText) from.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("linux/amd64");
                assertThat(((Dockerfile.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Dockerfile.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
            })
          )
        );
    }

    @Test
    void fromWithAs() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04 AS base
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Dockerfile.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Dockerfile.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getAs()).isNotNull();
                assertThat(((Dockerfile.PlainText) from.getAs().getName().getContents().getFirst()).getText()).isEqualTo("base");
            })
          )
        );
    }

    @Test
    void simpleRun() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Run run = (Dockerfile.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Dockerfile.ShellForm.class);
                Dockerfile.ShellForm shellForm = (Dockerfile.ShellForm) run.getCommandLine().getForm();
                assertThat(shellForm.getArguments())
                  .singleElement()
                  .extracting(arg -> ((Dockerfile.PlainText) arg.getContents().getFirst()).getText())
                  .isEqualTo("apt-get update");
            })
          )
        );
    }

    @Test
    void runExecForm() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN ["apt-get", "update"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Run run = (Dockerfile.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Dockerfile.ExecForm.class);
                Dockerfile.ExecForm execForm = (Dockerfile.ExecForm) run.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(2)
                  .satisfiesExactly(
                    arg -> assertThat(((Dockerfile.QuotedString) arg.getContents().getFirst()).getValue()).isEqualTo("apt-get"),
                    arg -> assertThat(((Dockerfile.QuotedString) arg.getContents().getFirst()).getValue()).isEqualTo("update")
                  );
            })
          )
        );
    }

    @Test
    void runWithEnvironmentVariable() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN CGO_ENABLED=0 go build -o app
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Run run = (Dockerfile.Run) doc.getStages().getLast().getInstructions().getFirst();
                assertThat(run.getFlags()).isNull();
                assertThat(run.getCommandLine().getForm()).isInstanceOf(Dockerfile.ShellForm.class);
                Dockerfile.ShellForm shellForm = (Dockerfile.ShellForm) run.getCommandLine().getForm();
                assertThat(shellForm.getArguments())
                  .singleElement()
                  .extracting(arg -> ((Dockerfile.PlainText) arg.getContents().getFirst()).getText())
                  .isEqualTo("CGO_ENABLED=0 go build -o app");
            })
          )
        );
    }

    @Test
    void multipleInstructions() {
        rewriteRun(
          dockerfile(
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
          dockerfile(
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
          dockerfile(
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
          dockerfile(
            """
              FROM golang:1.20 AS builder
              RUN go build -o app .
              
              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getStages())
              .satisfiesExactly(
                golang -> assertThat(((Dockerfile.PlainText) golang.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("golang"),
                alpine -> assertThat(((Dockerfile.PlainText) alpine.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("alpine")
              ))
          )
        );
    }

    @Test
    void runWithLineContinuation() {
        rewriteRun(
          dockerfile(
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
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN --network=none apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Run run = (Dockerfile.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getFlags()).hasSize(1);
                assertThat(run.getFlags().getFirst().getName()).isEqualTo("network");
                assertThat(((Dockerfile.PlainText) run.getFlags().getFirst().getValue().getContents().getFirst()).getText()).contains("none");
            })
          )
        );
    }

    @Test
    void runWithMultipleFlags() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN --network=none --mount=type=cache,target=/cache apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Run run = (Dockerfile.Run) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(run.getFlags()).hasSize(2);
                assertThat(run.getFlags().get(0).getName()).isEqualTo("network");
                assertThat(((Dockerfile.PlainText) run.getFlags().get(0).getValue().getContents().getFirst()).getText()).contains("none");
                assertThat(run.getFlags().get(1).getName()).isEqualTo("mount");
                assertThat(((Dockerfile.PlainText) run.getFlags().get(1).getValue().getContents().getFirst()).getText()).contains("type=cache,target=/cache");
            })
          )
        );
    }

    @Test
    void cmdShellForm() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              CMD nginx -g daemon off;
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Cmd cmd = (Dockerfile.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Dockerfile.ShellForm.class);
                Dockerfile.ShellForm shellForm = (Dockerfile.ShellForm) cmd.getCommandLine().getForm();
                assertThat(shellForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void cmdExecForm() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              CMD ["nginx", "-g", "daemon off;"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Cmd cmd = (Dockerfile.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Dockerfile.ExecForm.class);
                Dockerfile.ExecForm execForm = (Dockerfile.ExecForm) cmd.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(3);
            })
          )
        );
    }

    @Test
    void entrypointExecForm() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ENTRYPOINT ["./app"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Entrypoint entrypoint = (Dockerfile.Entrypoint) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(entrypoint.getCommandLine().getForm()).isInstanceOf(Dockerfile.ExecForm.class);
                Dockerfile.ExecForm execForm = (Dockerfile.ExecForm) entrypoint.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void entrypointShellForm() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ENTRYPOINT /bin/sh -c 'echo hello'
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Entrypoint entrypoint = (Dockerfile.Entrypoint) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(entrypoint.getCommandLine().getForm()).isInstanceOf(Dockerfile.ShellForm.class);
                Dockerfile.ShellForm shellForm = (Dockerfile.ShellForm) entrypoint.getCommandLine().getForm();
                assertThat(shellForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void envSingleLine() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0
              ENV PATH=/usr/local/bin:$PATH
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Dockerfile.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Dockerfile.Env env1 = (Dockerfile.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(((Dockerfile.PlainText) env1.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Dockerfile.PlainText) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envMultiplePairs() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0 NPM_VERSION=9.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Env env = (Dockerfile.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs()).hasSize(2);
                assertThat(((Dockerfile.PlainText) env.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Dockerfile.PlainText) env.getPairs().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
                assertThat(((Dockerfile.PlainText) env.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("NPM_VERSION");
                assertThat(((Dockerfile.PlainText) env.getPairs().get(1).getValue().getContents().getFirst()).getText()).isEqualTo("9.0.0");
            })
          )
        );
    }

    @Test
    void envOldStyleSpaceSeparated() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION 18.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Dockerfile.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Dockerfile.Env env1 = (Dockerfile.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(((Dockerfile.PlainText) env1.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("NODE_VERSION");
                assertThat(((Dockerfile.PlainText) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void labelSinglePair() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Label label = (Dockerfile.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(((Dockerfile.PlainText) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Dockerfile.PlainText) label.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void labelMultiplePairs() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0 app=myapp
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Label label = (Dockerfile.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(2);
                assertThat(((Dockerfile.PlainText) label.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Dockerfile.PlainText) label.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("app");
            })
          )
        );
    }

    @Test
    void labelWithQuotedValues() {
        rewriteRun(
          dockerfile(
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
          dockerfile(
            """
              FROM ubuntu:20.04
              ARG VERSION=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Arg arg = (Dockerfile.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) arg.getName().getContents().getFirst()).getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNotNull();
                assertThat(((Dockerfile.PlainText) arg.getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void argWithoutValue() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ARG VERSION
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Arg arg = (Dockerfile.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) arg.getName().getContents().getFirst()).getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNull();
            })
          )
        );
    }

    @Test
    void argInstructions() {
        rewriteRun(
          dockerfile(
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
          dockerfile(
            """
              FROM ubuntu:20.04
              WORKDIR /app
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Workdir workdir = (Dockerfile.Workdir) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) workdir.getPath().getContents().getFirst()).getText()).isEqualTo("/app");
            })
          )
        );
    }

    @Test
    void userInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              USER nobody
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.User user = (Dockerfile.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) user.getUser().getContents().getFirst()).getText()).isEqualTo("nobody");
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    @Test
    void userWithGroup() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              USER app:group
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.User user = (Dockerfile.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) user.getUser().getContents().getFirst()).getText()).isEqualTo("app");
                assertThat(((Dockerfile.PlainText) user.getGroup().getContents().getFirst()).getText()).isEqualTo("group");
            })
          )
        );
    }

    @Test
    void stopsignalInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              STOPSIGNAL SIGTERM
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Stopsignal stopsignal = (Dockerfile.Stopsignal) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Dockerfile.PlainText) stopsignal.getSignal().getContents().getFirst()).getText()).isEqualTo("SIGTERM");
            })
          )
        );
    }

    @Test
    void maintainerInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              MAINTAINER John Doe <john@example.com>
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Maintainer maintainer = (Dockerfile.Maintainer) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(((Dockerfile.PlainText) maintainer.getText().getContents().getFirst()).getText()).isEqualTo("John Doe <john@example.com>");
            })
          )
        );
    }

    @Test
    void simpleCopy() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Copy copy = (Dockerfile.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getSources()).hasSize(1);
                assertThat(((Dockerfile.PlainText) copy.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.jar");
                assertThat(((Dockerfile.PlainText) copy.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void addInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ADD app.tar.gz /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Add add = (Dockerfile.Add) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(add.getSources()).hasSize(1);
                assertThat(((Dockerfile.PlainText) add.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.tar.gz");
                assertThat(((Dockerfile.PlainText) add.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void exposeInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              EXPOSE 8080
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Expose expose = (Dockerfile.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(1);
                assertThat(((Dockerfile.PlainText) expose.getPorts().getFirst().getContents().getFirst()).getText()).isEqualTo("8080");
            })
          )
        );
    }

    @Test
    void exposeMultiplePorts() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              EXPOSE 8080 8443
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Expose expose = (Dockerfile.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(2);
                assertThat(((Dockerfile.PlainText) expose.getPorts().get(0).getContents().getFirst()).getText()).isEqualTo("8080");
                assertThat(((Dockerfile.PlainText) expose.getPorts().get(1).getContents().getFirst()).getText()).isEqualTo("8443");
            })
          )
        );
    }

    @Test
    void volumeWithJsonArray() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              VOLUME ["/data", "/logs"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Volume volume = (Dockerfile.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isTrue();
            })
          )
        );
    }

    @Test
    void volumeWithPathList() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              VOLUME /data /logs
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Volume volume = (Dockerfile.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isFalse();
                assertThat(((Dockerfile.PlainText) volume.getValues().get(0).getContents().getFirst()).getText()).isEqualTo("/data");
                assertThat(((Dockerfile.PlainText) volume.getValues().get(1).getContents().getFirst()).getText()).isEqualTo("/logs");
            })
          )
        );
    }

    @Test
    void shellInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              SHELL ["/bin/bash", "-c"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Shell shell = (Dockerfile.Shell) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(shell.getArguments()).hasSize(2);
            })
          )
        );
    }

    @Test
    void onbuildInstruction() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ONBUILD RUN apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Onbuild onbuild = (Dockerfile.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Dockerfile.Run.class);
            })
          )
        );
    }

    @Test
    void onbuildWithCopy() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              ONBUILD COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Onbuild onbuild = (Dockerfile.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Dockerfile.Copy.class);
            })
          )
        );
    }

    @Test
    void healthcheckNone() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              HEALTHCHECK NONE
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Healthcheck healthcheck = (Dockerfile.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isTrue();
            })
          )
        );
    }

    @Test
    void healthcheckWithCmd() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.Healthcheck healthcheck = (Dockerfile.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isFalse();
                assertThat(healthcheck.getCmd()).isNotNull();
            })
          )
        );
    }

    @Test
    void copyInstructions() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              COPY --chown=app:app app.jar /app/
              COPY --from=builder /build/output /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Dockerfile.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Dockerfile.Copy copy1 = (Dockerfile.Copy) instructions.getFirst();
                assertThat(copy1.getFlags()).hasSize(1);
                assertThat(copy1.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Dockerfile.PlainText) copy1.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("app:app");

                Dockerfile.Copy copy2 = (Dockerfile.Copy) instructions.get(1);
                assertThat(copy2.getFlags()).hasSize(1);
                assertThat(copy2.getFlags().getFirst().getName()).isEqualTo("from");
                assertThat(((Dockerfile.PlainText) copy2.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("builder");
            })
          )
        );
    }

    @Test
    void complexExpression() {
        rewriteRun(
          dockerfile(
            """
              ARG VERSION=25
              FROM $REGISTRY/image:${VERSION}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Dockerfile.From from = doc.getStages().getLast().getFrom();

                // Check imageName contents
                List<Dockerfile.ArgumentContent> imageNameContents = from.getImageName().getContents();
                assertThat(imageNameContents).hasSize(2);
                assertThat(imageNameContents.getFirst()).extracting(arg -> ((Dockerfile.EnvironmentVariable) arg).getName()).isEqualTo("REGISTRY");
                assertThat(imageNameContents.get(1)).extracting(arg -> ((Dockerfile.PlainText) arg).getText()).isEqualTo("/image");

                // Check tag contents
                assertThat(from.getTag()).isNotNull();
                List<Dockerfile.ArgumentContent> tagContents = from.getTag().getContents();
                assertThat(tagContents).hasSize(2);
                assertThat(tagContents.getFirst()).extracting(arg -> ((Dockerfile.EnvironmentVariable) arg).getName()).isEqualTo("VERSION");
                assertThat(tagContents.get(1)).extracting(arg -> ((Dockerfile.PlainText) arg).getText()).isEqualTo("-suffix");

                // Check no digest
                assertThat(from.getDigest()).isNull();
            })
          )
        );
    }

    @Test
    void comprehensiveDockerfile() {
        rewriteRun(
          dockerfile(
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
          dockerfile(
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
          dockerfile(
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
          dockerfile(
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
