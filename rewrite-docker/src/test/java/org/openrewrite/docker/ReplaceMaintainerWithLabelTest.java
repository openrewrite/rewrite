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

class ReplaceMaintainerWithLabelTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceMaintainerWithLabel());
    }

    @DocumentExample
    @Test
    void replaceMaintainer() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              MAINTAINER Jane <jane@x.io>
              RUN make build
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="Jane <jane@x.io>"
              RUN make build
              """
          )
        );
    }

    @Test
    void noMaintainer() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="Jane <jane@x.io>"
              RUN make build
              """
          )
        );
    }

    @Test
    void preserveCommentsAndPrefix() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04

              # who to blame
              MAINTAINER jane@x.io
              RUN make build
              """,
            """
              FROM ubuntu:22.04

              # who to blame
              LABEL org.opencontainers.image.authors=jane@x.io
              RUN make build
              """
          )
        );
    }

    @Test
    void dropWhenTheStageAlreadyHasTheLabel() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="Jane <jane@x.io>"
              MAINTAINER Jane <jane@x.io>
              RUN make build
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="Jane <jane@x.io>"
              RUN make build
              """
          )
        );
    }

    @Test
    void multiStage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              MAINTAINER Jane <jane@x.io>
              RUN make build

              FROM ubuntu:22.04
              maintainer John <john@x.io>
              COPY --from=builder /app /app
              """,
            """
              FROM ubuntu:22.04 AS builder
              LABEL org.opencontainers.image.authors="Jane <jane@x.io>"
              RUN make build

              FROM ubuntu:22.04
              label org.opencontainers.image.authors="John <john@x.io>"
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void escapeWhatTheShellWouldRead() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              MAINTAINER The "A" Team <$TEAM>
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="The \\"A\\" Team <\\$TEAM>"
              """
          )
        );
    }

    @Test
    void leaveAFileDeclaringABacktickEscape() {
        rewriteRun(
          docker(
            """
              # escape=`
              FROM ubuntu:22.04
              MAINTAINER The "A" Team <$TEAM>
              """
          )
        );
    }

    @Test
    void replaceUnderABackslashEscapeDeclaredOutright() {
        rewriteRun(
          docker(
            """
              # escape=\\
              FROM ubuntu:22.04
              MAINTAINER The "A" Team <$TEAM>
              """,
            """
              # escape=\\
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="The \\"A\\" Team <\\$TEAM>"
              """
          )
        );
    }

    @Test
    void theLastMaintainerIsTheAuthor() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              MAINTAINER Jane <jane@x.io>
              MAINTAINER John <john@x.io>
              RUN make build
              """,
            """
              FROM ubuntu:22.04
              LABEL org.opencontainers.image.authors="John <john@x.io>"
              RUN make build
              """
          )
        );
    }
}
