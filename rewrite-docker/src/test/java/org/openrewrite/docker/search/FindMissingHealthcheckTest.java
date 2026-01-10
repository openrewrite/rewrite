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

class FindMissingHealthcheckTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindMissingHealthcheck());
    }

    @DocumentExample
    @Test
    void detectMissingHealthcheck() {
        rewriteRun(
          Assertions.docker(
            """
              FROM alpine:3.18
              CMD ["./app"]
              """,
            """
              ~~(Missing HEALTHCHECK instruction)~~>FROM alpine:3.18
              CMD ["./app"]
              """
          )
        );
    }

    @Test
    void healthcheckPresent() {
        rewriteRun(
          Assertions.docker(
            """
              FROM alpine:3.18
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              CMD ["./app"]
              """
          )
        );
    }

    @Test
    void healthcheckNonePresent() {
        rewriteRun(
          Assertions.docker(
            """
              FROM alpine:3.18
              HEALTHCHECK NONE
              CMD ["./app"]
              """
          )
        );
    }

    @Test
    void multiStageOnlyChecksFinalStage() {
        rewriteRun(
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void multiStageDetectsMissingHealthcheckInFinalStage() {
        rewriteRun(
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              HEALTHCHECK CMD go test
              RUN go build -o app .

              FROM alpine:3.18
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              HEALTHCHECK CMD go test
              RUN go build -o app .

              ~~(Missing HEALTHCHECK instruction)~~>FROM alpine:3.18
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void singleStageWithHealthcheck() {
        rewriteRun(
          Assertions.docker(
            """
              FROM nginx:1.25
              COPY nginx.conf /etc/nginx/nginx.conf
              HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1
              EXPOSE 80
              """
          )
        );
    }
}
