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

class LowerCaseStageNamesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LowerCaseStageNames());
    }

    @DocumentExample
    @Test
    void renameStageAndTheCopyThatReferencesIt() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS Builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=Builder /app /app
              """,
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void alreadyLowerCase() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void renameTheFromThatBuildsOnTheStage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS Base
              RUN apt-get update

              FROM Base AS Builder
              RUN make build

              FROM Base
              COPY --from=Builder /app /app
              """,
            """
              FROM ubuntu:22.04 AS base
              RUN apt-get update

              FROM base AS builder
              RUN make build

              FROM base
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void leaveAnImageOfTheSameNameAlone() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              COPY --from=nginx:latest /etc/nginx /etc/nginx

              FROM ubuntu:22.04 AS Nginx
              RUN make build
              """,
            """
              FROM ubuntu:22.04
              COPY --from=nginx:latest /etc/nginx /etc/nginx

              FROM ubuntu:22.04 AS nginx
              RUN make build
              """
          )
        );
    }

    @Test
    void leaveAStageWhoseLowerCaseNameIsTakenAlone() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04 AS Builder
              RUN make test

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void renameTheStageAMountReferences() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS Builder
              RUN make build

              FROM ubuntu:22.04
              RUN --mount=type=bind,from=Builder,source=/app,target=/app cp -r /app /out
              """,
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04
              RUN --mount=type=bind,from=builder,source=/app,target=/app cp -r /app /out
              """
          )
        );
    }

    @Test
    void numericStageReferenceIsUntouched() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS Builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=0 /app /app
              """,
            """
              FROM ubuntu:22.04 AS builder
              RUN make build

              FROM ubuntu:22.04
              COPY --from=0 /app /app
              """
          )
        );
    }
}
