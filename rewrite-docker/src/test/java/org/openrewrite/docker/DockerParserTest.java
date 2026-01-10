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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

/**
 * General integration tests for the Docker parser.
 * Instruction-specific tests are located in the tree package.
 */
class DockerParserTest implements RewriteTest {

    @Test
    void multipleInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void comprehensiveDockerfile() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1

              # Build stage
              FROM golang:1.20 AS builder
              WORKDIR /build
              COPY go.mod go.sum ./
              RUN go mod download
              COPY . .
              RUN CGO_ENABLED=0 go build -o app

              # Runtime stage
              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              WORKDIR /app
              COPY --from=builder /build/app .
              EXPOSE 8080
              USER nobody
              ENTRYPOINT ["./app"]
              """
          )
        );
    }

    @Test
    void multipleJsonArrayWhitespaceStyles() {
        // Test different whitespace styles in JSON arrays are preserved
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              CMD ["no-spaces"]
              ENTRYPOINT [ "with-spaces" ]
              """
          )
        );
    }
}
