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
import static org.openrewrite.test.TypeValidation.all;

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

    @Test
    void healthcheckWithIntervalFlag() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK --interval=30s CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                var healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isFalse();
                assertThat(healthcheck.getFlags()).hasSize(1);
                var flag = healthcheck.getFlags().getFirst();
                assertThat(flag.getName()).isEqualTo("interval");
                assertThat(flag.getValue()).isNotNull();
                assertThat(flag.getValue().getContents()).isNotEmpty();
                assertThat(((Docker.Literal) flag.getValue().getContents().getFirst()).getText()).isEqualTo("30s");
            })
          )
        );
    }

    @Test
    void healthcheckWithAllFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --start-interval=1s --retries=3 CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                var healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(healthcheck.isNone()).isFalse();
                assertThat(healthcheck.getFlags()).hasSize(5);
                assertThat(healthcheck.getFlags().get(0).getName()).isEqualTo("interval");
                assertThat(healthcheck.getFlags().get(1).getName()).isEqualTo("timeout");
                assertThat(healthcheck.getFlags().get(2).getName()).isEqualTo("start-period");
                assertThat(healthcheck.getFlags().get(3).getName()).isEqualTo("start-interval");
                assertThat(healthcheck.getFlags().get(4).getName()).isEqualTo("retries");
                var command = (Docker.ShellForm) healthcheck.getCmd().getCommand();
                assertThat(command.getArgument().getText()).isEqualTo("curl -f http://localhost/ || exit 1");
            })
          )
        );
    }

    @Test
    void healthcheckWithLineContinuationInCommand() {
        // Line continuation in the command itself (after CMD)
        rewriteRun(
          spec -> spec.typeValidationOptions(all().allowNonWhitespaceInWhitespace(true)),
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK --interval=5m --timeout=3s \\
                CMD curl -f http://localhost/ || exit 1
              """,
            spec -> spec.afterRecipe(doc -> {
                var healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();

                Docker.Flag interval = healthcheck.getFlags().getFirst();
                assertThat(interval.getName()).isEqualTo("interval");
                assertThat(((Docker.Literal)interval.getValue().getContents().getFirst()).getText()).isEqualTo("5m");

                Docker.Flag timeout = healthcheck.getFlags().getLast();
                assertThat(timeout.getName()).isEqualTo("timeout");
                assertThat(((Docker.Literal)timeout.getValue().getContents().getFirst()).getText()).isEqualTo("3s");

                var command = (Docker.ShellForm) healthcheck.getCmd().getCommand();
                assertThat(command.getArgument().getText()).isEqualTo("curl -f http://localhost/ || exit 1");
            })
          )
        );
    }

    @Test
    void healthcheckWithExecForm() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK --interval=30s CMD ["curl", "-f", "http://localhost/"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var healthcheck = (Docker.Healthcheck) doc.getStages().getFirst().getInstructions().getLast();

                Docker.Flag interval = healthcheck.getFlags().getFirst();
                assertThat(interval.getName()).isEqualTo("interval");
                assertThat(((Docker.Literal)interval.getValue().getContents().getFirst()).getText()).isEqualTo("30s");

                assertThat(healthcheck.getCmd()).isNotNull();
                assertThat(healthcheck.getCmd().getCommand()).isInstanceOf(Docker.ExecForm.class);
                var execForm = (Docker.ExecForm) healthcheck.getCmd().getCommand();
                assertThat(execForm.getArguments()).hasSize(3);
            })
          )
        );
    }

    @Test
    void healthcheckNoneWithMultipleSpaces() {
        // HEALTHCHECK with multiple spaces between keyword and NONE
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK  NONE
              """
          )
        );
    }

    @Test
    void healthcheckNoneWithTab() {
        // HEALTHCHECK with tab between keyword and NONE
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK\tNONE
              """
          )
        );
    }

    @Test
    void healthcheckCmdWithMultipleSpaces() {
        // HEALTHCHECK with multiple spaces between keyword and CMD
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK  CMD curl -f http://localhost/
              """
          )
        );
    }

    @Test
    void healthcheckCmdWithTab() {
        // HEALTHCHECK with tab between keyword and CMD
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              HEALTHCHECK\tCMD curl -f http://localhost/
              """
          )
        );
    }
}
