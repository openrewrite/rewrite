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

class RemoveRedundantTargetPlatformTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveRedundantTargetPlatform());
    }

    @DocumentExample
    @Test
    void removeTargetPlatform() {
        rewriteRun(
          docker(
            """
              FROM --platform=$TARGETPLATFORM ubuntu:22.04
              RUN make build
              """,
            """
              FROM ubuntu:22.04
              RUN make build
              """
          )
        );
    }

    @Test
    void removeBracedTargetPlatform() {
        rewriteRun(
          docker(
            """
              FROM --platform=${TARGETPLATFORM} ubuntu:22.04 AS builder
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
    void leaveAConstantPlatformAlone() {
        rewriteRun(
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:22.04
              RUN make build
              """
          )
        );
    }

    @Test
    void leaveBuildPlatformAlone() {
        rewriteRun(
          docker(
            """
              FROM --platform=$BUILDPLATFORM golang:1.22 AS builder
              RUN go build

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void noPlatform() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
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
              FROM --platform=$BUILDPLATFORM golang:1.22 AS builder
              RUN go build

              FROM --platform=$TARGETPLATFORM ubuntu:22.04
              COPY --from=builder /app /app
              """,
            """
              FROM --platform=$BUILDPLATFORM golang:1.22 AS builder
              RUN go build

              FROM ubuntu:22.04
              COPY --from=builder /app /app
              """
          )
        );
    }
}
