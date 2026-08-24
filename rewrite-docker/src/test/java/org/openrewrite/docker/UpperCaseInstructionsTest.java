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

class UpperCaseInstructionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpperCaseInstructions(null));
    }

    @DocumentExample
    @Test
    void upperCaseEveryKeyword() {
        rewriteRun(
          docker(
            """
              from ubuntu:22.04
              Run apt-get update
              workdir /app
              copy . .
              cmd ["/app/server"]
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update
              WORKDIR /app
              COPY . .
              CMD ["/app/server"]
              """
          )
        );
    }

    @Test
    void alreadyUpperCase() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              RUN make build
              ENV JAVA_OPTS="-Xmx1g"

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              ENTRYPOINT ["/app/server"]
              """
          )
        );
    }

    @Test
    void multiStage() {
        rewriteRun(
          docker(
            """
              arg VERSION=22.04
              from ubuntu:${VERSION} as builder
              run make build

              FROM ubuntu:${VERSION}
              copy --from=builder /app /app
              user appuser
              entrypoint ["/app/server"]
              """,
            """
              ARG VERSION=22.04
              FROM ubuntu:${VERSION} AS builder
              RUN make build

              FROM ubuntu:${VERSION}
              COPY --from=builder /app /app
              USER appuser
              ENTRYPOINT ["/app/server"]
              """
          )
        );
    }

    @Test
    void upperCasesTheAsOfAFrom() {
        rewriteRun(
          docker(
            """
              from ubuntu:22.04 as builder
              RUN make build
              """,
            """
              FROM ubuntu:22.04 AS builder
              RUN make build
              """
          )
        );
    }

    @Test
    void nestedInOnbuildAndHealthcheck() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              onbuild run make build
              healthcheck --interval=30s cmd curl -f http://localhost/
              """,
            """
              FROM ubuntu:22.04
              ONBUILD RUN make build
              HEALTHCHECK --interval=30s CMD curl -f http://localhost/
              """
          )
        );
    }

    @Test
    void lowerCase() {
        rewriteRun(
          spec -> spec.recipe(new UpperCaseInstructions("lowercase")),
          docker(
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """,
            """
              from ubuntu:22.04 as builder
              run make build

              from ubuntu:22.04
              copy --from=builder /app /app
              """
          )
        );
    }

    @Test
    void alreadyLowerCase() {
        rewriteRun(
          spec -> spec.recipe(new UpperCaseInstructions("lowercase")),
          docker(
            """
              from ubuntu:22.04 as builder
              run make build
              """
          )
        );
    }
}
