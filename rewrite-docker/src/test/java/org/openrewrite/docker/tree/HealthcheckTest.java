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
package org.openrewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class HealthcheckTest implements RewriteTest {

    @Test
    void healthcheckNone() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK NONE
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Healthcheck healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isTrue();
                assertThat(healthcheck.getCmd()).isNull();
            })
          )
        );
    }

    @Test
    void healthcheckWithCmd() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Healthcheck healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isFalse();
                assertThat(healthcheck.getCmd()).isNotNull();
                var command = (Docker.ShellForm) healthcheck.getCmd().getCommand();
                assertThat(command.getArgument().getText()).isEqualTo("curl -f http://localhost/ || exit 1");
            })
          )
        );
    }

    @Test
    void multiStageWithHealthcheckFollowedByCopy() {
        // Test that HEALTHCHECK is correctly parsed when followed by COPY --from
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18
              HEALTHCHECK CMD curl -f http://localhost/ || exit 1
              COPY --from=builder /app /app
              """,
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getStages()).hasSize(2);
                // Final stage should have HEALTHCHECK as first instruction and COPY as second
                Docker.Stage finalStage = doc.getStages().get(1);
                assertThat(finalStage.getInstructions().getFirst()).isInstanceOf(Docker.Healthcheck.class);
                assertThat(finalStage.getInstructions().getLast()).isInstanceOf(Docker.Copy.class);
            })
          )
        );
    }
}
