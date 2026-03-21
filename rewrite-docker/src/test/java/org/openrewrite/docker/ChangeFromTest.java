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

class ChangeFromTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeFrom("ubuntu", "20.04", null, null, "ubuntu", "22.04", null, null));
    }

    @DocumentExample
    @Test
    void changeSimpleBaseImage() {
        rewriteRun(
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
    void changeBaseImageWithFlags() {
        rewriteRun(
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
    void changeBaseImageWithAs() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("golang", "1.20", null, null, "golang", "1.21", null, null)),
          docker(
            """
              FROM golang:1.20 AS builder
              RUN go build -o app .

              FROM alpine:latest
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:latest
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void changeMultipleStages() {
        rewriteRun(
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
    void dontChangeNonMatchingImage() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              """
          )
        );
    }

    @Test
    void changeWithGlobPattern() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, "ubuntu", "22.04", null, null)),
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
    void changeWithGlobPatternMultipleMatches() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, "ubuntu", "22.04", null, null)),
          docker(
            """
              FROM ubuntu:18.04 AS base
              FROM ubuntu:20.04 AS builder
              FROM alpine:latest
              """,
            """
              FROM ubuntu:22.04 AS base
              FROM ubuntu:22.04 AS builder
              FROM alpine:latest
              """
          )
        );
    }

    @Test
    void changeWithWildcardImageName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("*/ubuntu", "20.04", null, null, "docker.io/library/ubuntu", "22.04", null, null)),
          docker(
            """
              FROM gcr.io/ubuntu:20.04
              """,
            """
              FROM docker.io/library/ubuntu:22.04
              """
          )
        );
    }

    @Test
    void addPlatformFlag() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, null, "ubuntu", "20.04", null, "linux/arm64")),
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM --platform=linux/arm64 ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void updatePlatformFlag() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, null, "ubuntu", "20.04", null, "linux/arm64")),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM --platform=linux/arm64 ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void removePlatformFlag() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, "linux/amd64", "ubuntu", null, null, "")),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeImageAndPlatform() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, null, "ubuntu", "22.04", null, "linux/arm64")),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              RUN apt-get update
              """,
            """
              FROM --platform=linux/arm64 ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void onlyChangeImageWithMatchingPlatform() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, "linux/amd64", "ubuntu", "22.04", null, "")),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              RUN apt-get update

              FROM --platform=linux/arm64 ubuntu:20.04
              RUN apt-get install -y nginx
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update

              FROM --platform=linux/arm64 ubuntu:20.04
              RUN apt-get install -y nginx
              """
          )
        );
    }

    @Test
    void changeImageAndPlatformWhenMatchingOldPlatform() {
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", null, "linux/amd64", "ubuntu", "22.04", null, "linux/arm64")),
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              RUN apt-get update

              FROM ubuntu:20.04
              RUN apt-get install -y nginx
              """,
            """
              FROM --platform=linux/arm64 ubuntu:22.04
              RUN apt-get update

              FROM ubuntu:20.04
              RUN apt-get install -y nginx
              """
          )
        );
    }

    @Test
    void changeBaseImageWithDoubleQuotedString() {
        // Quoted strings are parsed as a single unit, so we match the full image reference as the image name
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu:20.04", null, null, null, "ubuntu", "22.04", null, null)),
          docker(
            """
              FROM "ubuntu:20.04"
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
    void changeBaseImageWithDoubleQuotedStringPreservesAs() {
        // Quoted strings are parsed as a single unit, so we match the full image reference as the image name
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu:20.04", null, null, null, "ubuntu", "22.04", null, null)),
          docker(
            """
              FROM "ubuntu:20.04" as builder
              RUN apt-get update
              """,
            """
              FROM "ubuntu:22.04" as builder
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeBaseImageWithSingleQuotedString() {
        // Quoted strings are parsed as a single unit, so we match the full image reference as the image name
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu:20.04", null, null, null, "ubuntu", "22.04", null, null)),
          docker(
            """
              FROM 'ubuntu:20.04'
              RUN apt-get update
              """,
            """
              FROM 'ubuntu:22.04'
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void changeBaseImageWithSingleQuotedStringPreservesTrailingComment() {
        // Quoted strings are parsed as a single unit, so we match the full image reference as the image name
        rewriteRun(
          spec -> spec.recipe(new ChangeFrom("ubuntu:20.04", null, null, null, "ubuntu", "22.04", null, null)),
          docker(
            """
              FROM 'ubuntu:20.04' # Trailing comment
              RUN apt-get update
              """,
            """
              FROM 'ubuntu:22.04' # Trailing comment
              RUN apt-get update
              """
          )
        );
    }

    @Nested
    class EnvironmentVariables implements RewriteTest {

        @Test
        void matchesImageWithEnvironmentVariableInTag() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, "ubuntu", "22.04", null, null)),
              docker(
                """
                  FROM ubuntu:${TAG}
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
        void matchesImageWithEnvironmentVariableInDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM ubuntu@${DIGEST}
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
        void matchesImageWithEnvironmentVariableInName() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("*", "20.04", null, null, "ubuntu", "22.04", null, null)),
              docker(
                """
                  FROM ${IMAGE_NAME}:20.04
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
        void matchesBothLiteralAndEnvironmentVariableImages() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, "ubuntu", "22.04", null, null)),
              docker(
                """
                  FROM ubuntu:20.04 AS base
                  FROM ubuntu:${TAG} AS builder
                  """,
                """
                  FROM ubuntu:22.04 AS base
                  FROM ubuntu:22.04 AS builder
                  """
              )
            );
        }

        @Test
        void doesNotMatchWhenNeitherPatternNorImageCanMatch() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("alpine", null, null, null, "alpine", "3.18", null, null)),
              docker(
                """
                  FROM ubuntu:${TAG}
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void matchesFullyParameterizedImageWithWildcard() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("*", null, null, null, "ubuntu", "22.04", null, null)),
              docker(
                """
                  FROM ${REGISTRY}/${IMAGE}:${TAG}
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu:22.04
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class TagAndDigest implements RewriteTest {

        @Test
        void changeImageWithTagAndDigestToNewTag() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123def456
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
        void changeImageWithTagAndDigestToNewDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", "*", null, "ubuntu", "", "sha256:newdigest789", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123def456
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu@sha256:newdigest789
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void changeImageWithTagAndDigestToNewTagAndDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", "*", null, "ubuntu", "22.04", "sha256:newdigest789", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123def456
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu:22.04@sha256:newdigest789
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void matchImageWithTagAndDigestUsingWildcards() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "*", "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123
                  FROM ubuntu:18.04@sha256:def456
                  FROM alpine:latest
                  """,
                """
                  FROM ubuntu:22.04
                  FROM ubuntu:22.04
                  FROM alpine:latest
                  """
              )
            );
        }

        @Test
        void changeImageWithTagAndDigestPreservesAs() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123 AS builder
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu:22.04 AS builder
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void changeImageWithTagAndDigestPreservesPlatform() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.04", "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM --platform=linux/amd64 ubuntu:20.04@sha256:abc123
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
        void changeImageWithRegistryTagAndDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("my.registry.com/ubuntu", "*", "*", null, "my.registry.com/ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM my.registry.com/ubuntu:20.04@sha256:abc123
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
        void noChangeWhenTagDoesNotMatch() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", "18.04", "*", null, "ubuntu", "22.04", "", null)),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class PreserveImageName implements RewriteTest {

        @Test
        void changeSimpleTag() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("ubuntu", "20.*", null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("ubuntu", "18.*", null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("my.registry.com/ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("*/ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("golang", null, null, null, null, "1.21", null, null)),
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
        void changeTagToDigest() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "", "sha256:abc123def456", null)),
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
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", "", null)),
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
        void addTagToUntaggedImage() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, null, "sha256:abc123", null)),
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

        @Test
        void matchAnyImage() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("*", "latest", null, null, null, "1.0", null, null)),
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
        void changeTagPreservesImageNameWithEnvVar() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("*/ubuntu", null, null, null, null, "22.04", null, null)),
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
        void noChangeWhenAlreadyHasDesiredTag() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", null, null)),
              docker(
                """
                  FROM ubuntu:22.04
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void changeTagInMultipleStages() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "22.04", null, null)),
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
              spec -> spec.recipe(new ChangeFrom("debian", null, null, null, null, "12", null, null)),
              docker(
                """
                  FROM ubuntu:20.04
                  RUN apt-get update
                  """
              )
            );
        }
    }

    @Nested
    class EmptyStringRemoval implements RewriteTest {

        @Test
        void removeTagWithEmptyString() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "", null, null)),
              docker(
                """
                  FROM ubuntu:20.04
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void removeDigestWithEmptyString() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, null, "", null)),
              docker(
                """
                  FROM ubuntu@sha256:abc123
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void removePlatformWithEmptyString() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, null, null, "")),
              docker(
                """
                  FROM --platform=linux/amd64 ubuntu:20.04
                  RUN apt-get update
                  """,
                """
                  FROM ubuntu:20.04
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void noChangeWhenTagAlreadyAbsent() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, "", null, null)),
              docker(
                """
                  FROM ubuntu
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void noChangeWhenDigestAlreadyAbsent() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFrom("ubuntu", null, null, null, null, null, "", null)),
              docker(
                """
                  FROM ubuntu:20.04
                  RUN apt-get update
                  """
              )
            );
        }
    }
}
