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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class ChangeBaseImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, null));
    }

    @DocumentExample
    @Test
    void changeSimpleBaseImage() {
        rewriteRun(
          Assertions.docker(
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
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("golang:1.20", "golang:1.21", null, null)),
          Assertions.docker(
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
          Assertions.docker(
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
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:*", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:*", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("*/ubuntu:20.04", "docker.io/library/ubuntu:22.04", null, null)),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:20.04", null, "linux/arm64")),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:20.04", null, "linux/arm64")),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:20.04", "linux/amd64", null)),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, "linux/arm64")),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", "linux/amd64", null)),
          Assertions.docker(
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
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", "linux/amd64", "linux/arm64")),
          Assertions.docker(
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
        rewriteRun(
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
        rewriteRun(
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
        rewriteRun(
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
        rewriteRun(
          spec -> spec.recipe(new ChangeBaseImage("ubuntu:20.04", "ubuntu:22.04", null, null)),
          Assertions.docker(
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
}
