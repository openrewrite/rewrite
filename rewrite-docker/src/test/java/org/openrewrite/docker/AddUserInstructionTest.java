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

class AddUserInstructionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddUserInstruction("appuser", null, null, null));
    }

    @DocumentExample
    @Test
    void addUserToFinalStage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update
              USER appuser
              """
          )
        );
    }

    @Test
    void addUserWithGroup() {
        rewriteRun(
          spec -> spec.recipe(new AddUserInstruction("appuser", "appgroup", null, null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update
              USER appuser:appgroup
              """
          )
        );
    }

    @Test
    void skipIfUserAlreadyExists() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              USER existinguser
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addEvenIfUserExists() {
        rewriteRun(
          spec -> spec.recipe(new AddUserInstruction("appuser", null, null, false)),
          docker(
            """
              FROM ubuntu:22.04
              USER root
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              USER root
              RUN apt-get update
              USER appuser
              """
          )
        );
    }

    @Test
    void onlyModifiesFinalStage() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              USER appuser
              """
          )
        );
    }

    @Test
    void addToNamedStage() {
        rewriteRun(
          spec -> spec.recipe(new AddUserInstruction("builduser", null, "builder", null)),
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .
              USER builduser

              FROM alpine:3.18
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void multipleStagesAddToFinal() {
        rewriteRun(
          docker(
            """
              FROM node:18 AS frontend
              RUN npm run build

              FROM golang:1.21 AS backend
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=frontend /dist /dist
              COPY --from=backend /app /app
              """,
            """
              FROM node:18 AS frontend
              RUN npm run build

              FROM golang:1.21 AS backend
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=frontend /dist /dist
              COPY --from=backend /app /app
              USER appuser
              """
          )
        );
    }
}
