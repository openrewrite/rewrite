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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class ChangeImageTagTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeSimpleTag() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM ubuntu:20.04
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
    void changeTagWithOldTagPattern() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", "20.*", null, "22.04", null)),
          docker(
            """
              FROM ubuntu:20.04
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
    void noChangeWhenOldTagPatternDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", "18.*", null, "22.04", null)),
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeTagPreservesImageNameWithRegistry() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("my.registry.com/ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM my.registry.com/ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM my.registry.com/ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeTagWithGlobImagePattern() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("*/ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM my.registry.com/ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM my.registry.com/ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeTagPreservesFlags() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
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
    void changeTagPreservesAsClause() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("golang", null, null, "1.21", null)),
          docker(
            """
              FROM golang:1.20 AS builder
              RUN go build -o app .
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .
              """
          )
        );
    }

    @Test
    void changeTagInMultipleStages() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM ubuntu:20.04 AS base
              RUN apt-get update

              FROM ubuntu:20.04 AS builder
              RUN apt-get install -y build-essential

              FROM alpine:latest
              COPY --from=builder /app /app
              """,
            """
              FROM ubuntu:22.04 AS base
              RUN apt-get update

              FROM ubuntu:22.04 AS builder
              RUN apt-get install -y build-essential

              FROM alpine:latest
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void noChangeWhenImageNameDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("debian", null, null, "12", null)),
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyHasDesiredTag() {
        rewriteRun(
          spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Nested
    class EnvironmentVariables {

        @Test
        void changeTagPreservesImageNameWithEnvVar() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("*/ubuntu", null, null, "22.04", null)),
              docker(
                """
                  ARG REGISTRY=docker.io
                  FROM ${REGISTRY}/ubuntu:20.04
                  RUN apt-get update
                  """,
                """
                  ARG REGISTRY=docker.io
                  FROM ${REGISTRY}/ubuntu:22.04
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void matchAnyImageWithEnvVar() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("*", null, null, "22.04", null)),
              docker(
                """
                  ARG IMAGE=ubuntu
                  FROM $IMAGE:20.04
                  RUN apt-get update
                  """,
                """
                  ARG IMAGE=ubuntu
                  FROM $IMAGE:22.04
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void changeTagWhenOldTagHasEnvVar() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", "*", null, "22.04", null)),
              docker(
                """
                  ARG TAG=20.04
                  FROM ubuntu:${TAG}
                  RUN apt-get update
                  """,
                """
                  ARG TAG=20.04
                  FROM ubuntu:22.04
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class Digests {

        @Test
        void changeTagToDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, null, "sha256:abc123def456")),
              docker(
                """
                  FROM ubuntu:20.04
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
        void changeDigestToTag() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
              docker(
                """
                  FROM ubuntu@sha256:olddigest123
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
        void changeDigestToNewDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, null, "sha256:newdigest456")),
              docker(
                """
                  FROM ubuntu@sha256:olddigest123
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu@sha256:newdigest456
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void changeDigestWithOldDigestPattern() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, "sha256:old*", null, "sha256:newdigest456")),
              docker(
                """
                  FROM ubuntu@sha256:olddigest123
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu@sha256:newdigest456
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void noChangeWhenOldDigestPatternDoesNotMatch() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, "sha256:other*", null, "sha256:newdigest456")),
              docker(
                """
                  FROM ubuntu@sha256:olddigest123
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void noChangeWhenAlreadyHasDesiredDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, null, "sha256:abc123")),
              docker(
                """
                  FROM ubuntu@sha256:abc123
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class UntaggedImages {

        @Test
        void addTagToUntaggedImage() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, "22.04", null)),
              docker(
                """
                  FROM ubuntu
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
        void addDigestToUntaggedImage() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", null, null, null, "sha256:abc123")),
              docker(
                """
                  FROM ubuntu
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu@sha256:abc123
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class WildcardImagePatterns {

        @Test
        void matchAnyImage() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("*", "latest", null, "1.0", null)),
              docker(
                """
                  FROM ubuntu:latest
                  FROM alpine:latest
                  FROM node:18
                  """,
                """
                  FROM ubuntu:1.0
                  FROM alpine:1.0
                  FROM node:18
                  """
              )
            );
        }

        @Test
        void matchImagesByGlobPattern() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("node*", null, null, "20", null)),
              docker(
                """
                  FROM node:18
                  FROM nodejs:16
                  FROM ubuntu:20.04
                  """,
                """
                  FROM node:20
                  FROM nodejs:20
                  FROM ubuntu:20.04
                  """
              )
            );
        }

        @Test
        void changeTagWithEnvVarInTagAndDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeImageTag("ubuntu", "*", null, "24.04", null)),
              docker(
                """
                  ARG TAG=22.04
                  FROM ubuntu:${TAG}@sha256:abc123
                  RUN apt-get update
                  """,
                """
                  ARG TAG=22.04
                  FROM ubuntu:24.04
                  RUN apt-get update
                  """
              )
            );
        }

    }
}
