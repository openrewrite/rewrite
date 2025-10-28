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
              """
          )
        );
    }

    @Test
    void fromWithPlatform() {
        rewriteRun(
          dockerfile(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              """
          )
        );
    }

    @Test
    void fromWithAs() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04 AS base
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
          )
        );
    }

    @Test
    void runWithFlagContainingComma() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              RUN --mount=type=cache,target=/cache apt-get update
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
              """
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
