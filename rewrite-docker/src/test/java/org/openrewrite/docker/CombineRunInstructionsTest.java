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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class CombineRunInstructionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CombineRunInstructions(null));
    }

    @DocumentExample
    @Test
    void combineTwoRunInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              RUN apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """
          )
        );
    }

    @Test
    void combineThreeRunInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              RUN apt-get install -y curl
              RUN apt-get clean
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && apt-get clean
              """
          )
        );
    }

    @Test
    void singleRunUnchanged() {
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
    void nonConsecutiveRunsNotCombined() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              COPY app.jar /app/
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void preserveRunWithFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt apt-get update
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void combineAfterPreservingFlaggedRun() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt apt-get update
              RUN apt-get install -y curl
              RUN apt-get clean
              """,
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt apt-get update
              RUN apt-get install -y curl && apt-get clean
              """
          )
        );
    }

    @Test
    void customSeparator() {
        rewriteRun(
          spec -> spec.recipe(new CombineRunInstructions(" ; ")),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              RUN apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update ; apt-get install -y curl
              """
          )
        );
    }

    @Test
    void multipleStages() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go mod download
              RUN go build -o app .

              FROM alpine:3.18
              RUN apk update
              RUN apk add --no-cache ca-certificates
              """,
            """
              FROM golang:1.21 AS builder
              RUN go mod download && go build -o app .

              FROM alpine:3.18
              RUN apk update && apk add --no-cache ca-certificates
              """
          )
        );
    }

    @Test
    void multipleGroupsOfConsecutiveRuns() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              RUN apt-get install -y curl
              COPY app.jar /app/
              RUN chmod +x /app/app.jar
              RUN chown appuser:appuser /app/app.jar
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              COPY app.jar /app/
              RUN chmod +x /app/app.jar && chown appuser:appuser /app/app.jar
              """
          )
        );
    }
}
