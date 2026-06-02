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

class OnbuildTest implements RewriteTest {

    @Test
    void onbuildInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ONBUILD RUN apt-get update
              """,
            spec -> spec.afterRecipe(doc -> {
                var onbuild = (Docker.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Docker.Run.class);
            })
          )
        );
    }

    @Test
    void onbuildWithCopy() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ONBUILD COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var onbuild = (Docker.Onbuild) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(onbuild.getInstruction()).isInstanceOf(Docker.Copy.class);
            })
          )
        );
    }
}
