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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class FindUnpinnedBaseImagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindUnpinnedBaseImages());
    }

    @DocumentExample
    @Test
    void detectLatestTag() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:latest
              RUN apt-get update
              """,
            """
              ~~(Uses 'latest' tag)~~>FROM ubuntu:latest
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectNoTag() {
        rewriteRun(
          docker(
            """
              FROM ubuntu
              RUN apt-get update
              """,
            """
              ~~(Uses implicit 'latest' tag)~~>FROM ubuntu
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void pinnedDigestIsOk() {
        rewriteRun(
          docker(
            """
              FROM ubuntu@sha256:abc123def456
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void pinnedTagIsOk() {
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
    void scratchImageIsOk() {
        rewriteRun(
          docker(
            """
              FROM scratch
              COPY app /app
              ENTRYPOINT ["/app"]
              """
          )
        );
    }

    @Test
    void multiStageMarksAllUnpinned() {
        rewriteRun(
          docker(
            """
              FROM golang AS builder
              RUN go build -o app .

              FROM alpine:latest
              COPY --from=builder /app /app
              """,
            """
              ~~(Uses implicit 'latest' tag)~~>FROM golang AS builder
              RUN go build -o app .

              ~~(Uses 'latest' tag)~~>FROM alpine:latest
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void multiStageWithMixedPinning() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              ~~(Uses implicit 'latest' tag)~~>FROM alpine
              COPY --from=builder /app /app
              """
          )
        );
    }
}
