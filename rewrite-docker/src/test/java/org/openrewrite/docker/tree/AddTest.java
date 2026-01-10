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

class AddTest implements RewriteTest {

    @Test
    void addInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD app.tar.gz /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Add add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(add.getSources()).hasSize(1);
                assertThat(((Docker.PlainText) add.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.tar.gz");
                assertThat(((Docker.PlainText) add.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }
}
