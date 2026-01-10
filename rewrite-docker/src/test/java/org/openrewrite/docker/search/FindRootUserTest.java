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
package org.openrewrite.docker.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.docker.Assertions;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class FindRootUserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindRootUser(null));
    }

    @DocumentExample
    @Test
    void detectMissingUser() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              ~~(No USER instruction, runs as root)~~>FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectExplicitRoot() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              USER root
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              ~~(Explicitly runs as root)~~>USER root
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectUserZero() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              USER 0
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              ~~(Explicitly runs as root)~~>USER 0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void nonRootUserIsOk() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              USER appuser
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void multiStageOnlyChecksFinalStage() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              USER appuser
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void multiStageDetectsMissingUserInFinalStage() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              USER builder
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              USER builder
              RUN go build -o app .

              ~~(No USER instruction, runs as root)~~>FROM alpine:3.18
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void disableMissingUserCheck() {
        rewriteRun(
          spec -> spec.recipe(new FindRootUser(false)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void disableMissingUserCheckStillFindsExplicitRoot() {
        rewriteRun(
          spec -> spec.recipe(new FindRootUser(false)),
          docker(
            """
              FROM ubuntu:22.04
              USER root
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              ~~(Explicitly runs as root)~~>USER root
              RUN apt-get update
              """
          )
        );
    }
}
