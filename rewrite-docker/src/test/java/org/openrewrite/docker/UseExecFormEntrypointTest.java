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

class UseExecFormEntrypointTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseExecFormEntrypoint(null, null));
    }

    @DocumentExample
    @Test
    void convertSimpleEntrypoint() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server
              """,
            """
              FROM ubuntu:22.04
              ENTRYPOINT ["/app/server"]
              """
          )
        );
    }

    @Test
    void convertEntrypointWithArguments() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server --port 8080
              """,
            """
              FROM ubuntu:22.04
              ENTRYPOINT ["/app/server", "--port", "8080"]
              """
          )
        );
    }

    @Test
    void convertSimpleCmd() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              CMD python app.py
              """,
            """
              FROM ubuntu:22.04
              CMD ["python", "app.py"]
              """
          )
        );
    }

    @Test
    void convertCmdWithArguments() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              CMD node server.js --production
              """,
            """
              FROM ubuntu:22.04
              CMD ["node", "server.js", "--production"]
              """
          )
        );
    }

    @Test
    void preserveAlreadyExecFormEntrypoint() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT ["/app/server"]
              """
          )
        );
    }

    @Test
    void preserveAlreadyExecFormCmd() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              CMD ["python", "app.py"]
              """
          )
        );
    }

    @Test
    void convertBothEntrypointAndCmd() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server
              CMD --config /etc/app.conf
              """,
            """
              FROM ubuntu:22.04
              ENTRYPOINT ["/app/server"]
              CMD ["--config", "/etc/app.conf"]
              """
          )
        );
    }

    @Test
    void onlyConvertEntrypoint() {
        rewriteRun(
          spec -> spec.recipe(new UseExecFormEntrypoint(true, false)),
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server
              CMD --config /etc/app.conf
              """,
            """
              FROM ubuntu:22.04
              ENTRYPOINT ["/app/server"]
              CMD --config /etc/app.conf
              """
          )
        );
    }

    @Test
    void onlyConvertCmd() {
        rewriteRun(
          spec -> spec.recipe(new UseExecFormEntrypoint(false, true)),
          docker(
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server
              CMD --config /etc/app.conf
              """,
            """
              FROM ubuntu:22.04
              ENTRYPOINT /app/server
              CMD ["--config", "/etc/app.conf"]
              """
          )
        );
    }

    @Test
    void handleQuotedArguments() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              CMD echo "Hello World"
              """,
            """
              FROM ubuntu:22.04
              CMD ["echo", "Hello World"]
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
              CMD go build -o app .

              FROM alpine:3.18
              ENTRYPOINT /app
              """,
            """
              FROM golang:1.21 AS builder
              CMD ["go", "build", "-o", "app", "."]

              FROM alpine:3.18
              ENTRYPOINT ["/app"]
              """
          )
        );
    }
}
