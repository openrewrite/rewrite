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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class EnvTest implements RewriteTest {

    @Test
    void envSingleLine() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0
              ENV PATH=/usr/local/bin:$PATH
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Env env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envMultiplePairs() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0 NPM_VERSION=9.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Env env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs()).hasSize(2);
                assertThat(env.getPairs().get(0).getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env.getPairs().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
                assertThat(env.getPairs().get(1).getKey().getText()).isEqualTo("NPM_VERSION");
                assertThat(((Docker.Literal) env.getPairs().get(1).getValue().getContents().getFirst()).getText()).isEqualTo("9.0.0");
            })
          )
        );
    }

    @Test
    void envOldStyleSpaceSeparated() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION 18.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Env env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(env1.getPairs().getFirst().getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envWithKeywordName() {
        // Test ENV where key is a Docker keyword (like SHELL)
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV SHELL /usr/bin/zsh
              """
          )
        );
    }
}
