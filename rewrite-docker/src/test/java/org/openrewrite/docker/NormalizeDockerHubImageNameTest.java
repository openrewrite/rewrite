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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class NormalizeDockerHubImageNameTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NormalizeDockerHubImageName());
    }

    @DocumentExample
    @Test
    void normalizeDockerIoLibraryPrefix() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void normalizeDockerIoUserImage() {
        rewriteRun(
          docker(
            """
              FROM docker.io/myuser/myimage:1.0
              RUN echo "hello"
              """,
            """
              FROM myuser/myimage:1.0
              RUN echo "hello"
              """
          )
        );
    }

    @Test
    void normalizeIndexDockerIoLibraryPrefix() {
        rewriteRun(
          docker(
            """
              FROM index.docker.io/library/nginx:latest
              EXPOSE 80
              """,
            """
              FROM nginx:latest
              EXPOSE 80
              """
          )
        );
    }

    @Test
    void normalizeRegistryHubDockerComPrefix() {
        rewriteRun(
          docker(
            """
              FROM registry.hub.docker.com/library/alpine:3.18
              RUN apk add --no-cache ca-certificates
              """,
            """
              FROM alpine:3.18
              RUN apk add --no-cache ca-certificates
              """
          )
        );
    }

    @Test
    void normalizeRegistry1DockerIoPrefix() {
        rewriteRun(
          docker(
            """
              FROM registry-1.docker.io/library/python:3.11
              RUN pip install requests
              """,
            """
              FROM python:3.11
              RUN pip install requests
              """
          )
        );
    }

    @Test
    void normalizeMultipleStages() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/golang:1.21 AS builder
              RUN go build -o app .

              FROM docker.io/library/alpine:3.18
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void preservePlatformFlag() {
        rewriteRun(
          docker(
            """
              FROM --platform=linux/amd64 docker.io/library/ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM --platform=linux/amd64 ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void preserveDigest() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/ubuntu@sha256:abc123def456
              RUN apt-get update
              """,
            """
              FROM ubuntu@sha256:abc123def456
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noChangeForAlreadyNormalizedImage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noChangeForNonDockerHubRegistry() {
        rewriteRun(
          docker(
            """
              FROM gcr.io/myproject/myimage:1.0
              RUN echo "hello"
              """
          )
        );
    }

    @Test
    void noChangeForPrivateRegistry() {
        rewriteRun(
          docker(
            """
              FROM my.private.registry.com/myimage:1.0
              RUN echo "hello"
              """
          )
        );
    }

    @Test
    void noChangeForScratchImage() {
        rewriteRun(
          docker(
            """
              FROM scratch
              COPY /app /app
              """
          )
        );
    }

    @Test
    void normalizeLibraryPrefixWithoutRegistry() {
        rewriteRun(
          docker(
            """
              FROM library/ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noChangeForAnOrganizationThatLooksLikeARegistry() {
        rewriteRun(
          docker(
            """
              FROM redhat/ubi9-minimal:9.4
              RUN microdnf update
              """
          )
        );
    }

    @Test
    void preservesAVariableReferenceInTheImageName() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/${BASE_IMAGE}:22.04
              RUN apt-get update
              """,
            """
              FROM ${BASE_IMAGE}:22.04
              RUN apt-get update
              """,
            spec -> spec.afterRecipe(file -> {
                Docker.From from = file.getStages().getFirst().getFrom();
                assertThat(from.getImageName().getContents())
                  .singleElement()
                  .isInstanceOfSatisfying(Docker.EnvironmentVariable.class, variable ->
                    assertThat(variable.getName()).isEqualTo("BASE_IMAGE"));
            })
          )
        );
    }

    @Test
    void normalizeCopyFrom() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              COPY --from=docker.io/library/alpine:3.19 /lib /app/lib
              """,
            """
              FROM ubuntu:22.04
              COPY --from=alpine:3.19 /lib /app/lib
              """
          )
        );
    }

    @Test
    void normalizeMountFrom() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN --mount=type=bind,from=docker.io/library/composer:2.8.3,source=/usr/bin/composer,target=/usr/bin/composer composer install
              """,
            """
              FROM ubuntu:22.04
              RUN --mount=type=bind,from=composer:2.8.3,source=/usr/bin/composer,target=/usr/bin/composer composer install
              """
          )
        );
    }

    @Test
    void mountFromAStageIsNotAnImageName() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/ubuntu:22.04 AS library
              FROM alpine:3.19
              RUN --mount=type=bind,from=library,source=/lib,target=/app/lib ls /app/lib
              """,
            """
              FROM ubuntu:22.04 AS library
              FROM alpine:3.19
              RUN --mount=type=bind,from=library,source=/lib,target=/app/lib ls /app/lib
              """
          )
        );
    }

    @Test
    void copyFromAStageIsNotAnImageName() {
        rewriteRun(
          docker(
            """
              FROM docker.io/library/ubuntu:22.04 AS library
              FROM alpine:3.19
              COPY --from=library /lib /app/lib
              """,
            """
              FROM ubuntu:22.04 AS library
              FROM alpine:3.19
              COPY --from=library /lib /app/lib
              """
          )
        );
    }

    @Nested
    class QuotedStrings implements RewriteTest {

        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new NormalizeDockerHubImageName());
        }

        @Test
        void normalizeDoubleQuotedImage() {
            rewriteRun(
              docker(
                """
                  FROM "docker.io/library/ubuntu:22.04"
                  RUN apt-get update
                  """,
                """
                  FROM "ubuntu:22.04"
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void normalizeSingleQuotedImage() {
            rewriteRun(
              docker(
                """
                  FROM 'docker.io/library/ubuntu:22.04'
                  RUN apt-get update
                  """,
                """
                  FROM 'ubuntu:22.04'
                  RUN apt-get update
                  """
              )
            );
        }
    }
}
