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

class AddOrUpdateLabelTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddOrUpdateLabel("version", "1.0.0", null, null));
    }

    @DocumentExample
    @Test
    void addNewLabel() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL version=1.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addLabelWithQuotedValue() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("description", "My awesome app", null, null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL description="My awesome app"
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void updateExistingLabel() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              LABEL version=0.9.0
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL version=1.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void skipExistingLabelWhenOverwriteFalse() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("version", "1.0.0", false, null)),
          docker(
            """
              FROM ubuntu:22.04
              LABEL version=0.9.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void updateLabelWithSeparateInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              LABEL author=dev
              LABEL version=0.5.0
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL author=dev
              LABEL version=1.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addToSpecificStage() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("version", "1.0.0", null, "final")),
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18 AS final
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18 AS final
              LABEL version=1.0.0
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void addOnlyToFinalStageByDefault() {
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
              LABEL version=1.0.0
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void insertAfterExistingLabels() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("newkey", "newvalue", null, null)),
          docker(
            """
              FROM ubuntu:22.04
              LABEL existing=value
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL existing=value
              LABEL newkey=newvalue
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void preserveOciLabelKey() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("org.opencontainers.image.version", "1.0.0", null, null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.version=1.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noChangeWhenStageNotFound() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateLabel("version", "1.0.0", null, "nonexistent")),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }
}
