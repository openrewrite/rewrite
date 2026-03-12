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

class ArgTest implements RewriteTest {

    @Test
    void simpleArg() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                var arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getName().getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNotNull();
                assertThat(((Docker.Literal) arg.getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void argWithoutValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION
              """,
            spec -> spec.afterRecipe(doc -> {
                var arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getName().getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNull();
            })
          )
        );
    }

    @Test
    void argInstructions() {
        rewriteRun(
          docker(
            """
              ARG BASE_IMAGE=ubuntu:20.04
              FROM ${BASE_IMAGE}
              ARG VERSION
              """
          )
        );
    }

    @Test
    void globalArg() {
        rewriteRun(
          docker(
            """
              ARG VERSION=25
              FROM ubuntu:${VERSION}
              """,
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getGlobalArgs()).hasSize(1);
                Docker.Arg globalArg = doc.getGlobalArgs().getFirst();
                assertThat(globalArg.getName().getText()).isEqualTo("VERSION");
            })
          )
        );
    }
}
