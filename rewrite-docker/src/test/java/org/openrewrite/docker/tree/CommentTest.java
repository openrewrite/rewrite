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

class CommentTest implements RewriteTest {

    @Test
    void commentsAtTop() {
        rewriteRun(
          docker(
            """
              # This is a comment
              # Another comment line
              FROM ubuntu:20.04
              """
          )
        );
    }

    /// Docker runs the `#` too: a comment is a line of its own, never the tail of one.
    @Test
    void hashAfterAnArgumentIsNoComment() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update  # this runs too
              """,
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().getFirst();
                assertThat(((Docker.ShellForm) run.getCommand()).getArgument().getText())
                  .isEqualTo("apt-get update  # this runs too");
            })
          )
        );
    }

    @Test
    void commentsBetweenInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              # Update and install dependencies
              RUN apt-get update
              # Install curl
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void emptyLinesAndComments() {
        rewriteRun(
          docker(
            """
              # Base image
              FROM ubuntu:20.04
              
              # System updates
              RUN apt-get update
              
              # Install packages
              RUN apt-get install -y curl wget
              """
          )
        );
    }

    /// Docker gives up on directives at the first comment or instruction.
    @Test
    void keyValueCommentIsNoDirectivePastTheHeadOfTheFile() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1
              FROM ubuntu:20.04
              # DEBIAN_FRONTEND=noninteractive keeps apt-get from asking
              RUN apt-get update
              ENV LANG=C.UTF-8
              """,
            spec -> spec.afterRecipe(file -> assertThat(file.getStages().getFirst().getInstructions())
              .satisfiesExactly(
                run -> assertThat(run).isInstanceOf(Docker.Run.class),
                env -> assertThat(env).isInstanceOf(Docker.Env.class)))
          )
        );
    }
}
