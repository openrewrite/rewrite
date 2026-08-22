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

class FixBuildKitLintWarningsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.docker.FixBuildKitLintWarnings");
    }

    @DocumentExample
    @Test
    void fixEveryWarning() {
        rewriteRun(
          docker(
            """
              from scratch as Builder
              maintainer Jane Doe <jane@example.io>
              ENV JAVA_OPTS -Xmx1g -Xms1g
              LABEL description An example image

              FROM --platform=$TARGETPLATFORM scratch
              copy --from=Builder /app /app
              """,
            """
              FROM scratch AS builder
              LABEL org.opencontainers.image.authors="Jane Doe <jane@example.io>"
              ENV JAVA_OPTS="-Xmx1g -Xms1g"
              LABEL description="An example image"

              FROM scratch
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void aFileWithNothingToWarnAbout() {
        rewriteRun(
          docker(
            """
              FROM scratch AS builder
              LABEL org.opencontainers.image.authors="Jane Doe <jane@example.io>"
              ENV JAVA_OPTS="-Xmx1g -Xms1g"

              FROM --platform=$BUILDPLATFORM scratch
              COPY --from=builder /app /app
              """
          )
        );
    }
}
