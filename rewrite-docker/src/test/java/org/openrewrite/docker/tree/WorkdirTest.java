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

class WorkdirTest implements RewriteTest {

    @Test
    void workdirInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              WORKDIR /app
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Workdir workdir = (Docker.Workdir) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.PlainText) workdir.getPath().getContents().getFirst()).getText()).isEqualTo("/app");
            })
          )
        );
    }
}
