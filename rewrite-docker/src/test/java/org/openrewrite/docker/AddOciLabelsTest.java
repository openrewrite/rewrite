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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class AddOciLabelsTest implements RewriteTest {

    @DocumentExample
    @Test
    void addTitleAndVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  "MyApplication",
                  null,
                  "1.0.0",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null
          )),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.title=MyApplication
              LABEL org.opencontainers.image.version=1.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addMultipleLabels() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  "MyApp",
                  "A containerized application",
                  "2.0.0",
                  "2024-01-15T10:30:00Z",
                  "abc123",
                  "https://github.com/myorg/myapp",
                  "https://myapp.example.com",
                  "MyOrganization",
                  "Apache-2.0",
                  "maintainers@example.com"
          )),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.title=MyApp
              LABEL org.opencontainers.image.description="A containerized application"
              LABEL org.opencontainers.image.version=2.0.0
              LABEL org.opencontainers.image.created=2024-01-15T10:30:00Z
              LABEL org.opencontainers.image.revision=abc123
              LABEL org.opencontainers.image.source=https://github.com/myorg/myapp
              LABEL org.opencontainers.image.url=https://myapp.example.com
              LABEL org.opencontainers.image.vendor=MyOrganization
              LABEL org.opencontainers.image.licenses=Apache-2.0
              LABEL org.opencontainers.image.authors=maintainers@example.com
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void updateExistingLabel() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  "UpdatedTitle",
                  null,
                  "2.0.0",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null
          )),
          docker(
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.title="Old Title"
              LABEL org.opencontainers.image.version=1.0.0
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.title=UpdatedTitle
              LABEL org.opencontainers.image.version=2.0.0
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addOnlyToFinalStage() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  "FinalImage",
                  null,
                  "1.0.0",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null
          )),
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
              LABEL org.opencontainers.image.title=FinalImage
              LABEL org.opencontainers.image.version=1.0.0
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void noChangesWhenNoLabelsSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null
          )),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void addSourceLabel() {
        rewriteRun(
          spec -> spec.recipe(new AddOciLabels(
                  null,
                  null,
                  null,
                  null,
                  null,
                  "https://github.com/openrewrite/rewrite",
                  null,
                  null,
                  null,
                  null
          )),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.source=https://github.com/openrewrite/rewrite
              RUN apt-get update
              """
          )
        );
    }
}
