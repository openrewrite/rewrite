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

class CopyTest implements RewriteTest {

    @Test
    void simpleCopy() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Copy copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getSources()).hasSize(1);
                assertThat(((Docker.Literal) copy.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.jar");
                assertThat(((Docker.Literal) copy.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void copyWithFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY --chown=app:app app.jar /app/
              COPY --from=builder /build/output /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                Docker.Copy copy1 = (Docker.Copy) instructions.getFirst();
                assertThat(copy1.getFlags()).hasSize(1);
                assertThat(copy1.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.Literal) copy1.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("app:app");

                Docker.Copy copy2 = (Docker.Copy) instructions.get(1);
                assertThat(copy2.getFlags()).hasSize(1);
                assertThat(copy2.getFlags().getFirst().getName()).isEqualTo("from");
                assertThat(((Docker.Literal) copy2.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("builder");
            })
          )
        );
    }

    @Test
    void copyWithHeredoc() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY <<EOF /app/config.txt
              # Configuration file
              setting1=value1
              setting2=value2
              EOF
              """
          )
        );
    }

    @Test
    void copyWithLinkAndFromFlags() {
        // Test COPY with both --link and --from flags (common pattern in multi-stage builds)
        rewriteRun(
          docker(
            """
              FROM scratch
              ENTRYPOINT [ "app.wasm" ]
              COPY --link --from=build /app.wasm /app.wasm
              """
          )
        );
    }

    @Test
    void heredocWithBash() {
        // Test heredoc with bash as command
        // Note: EOT must not be indented - heredoc terminators must match exactly
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOT bash
              set -ex
              apt-get update
              EOT
              """
          )
        );
    }
}
