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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

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
