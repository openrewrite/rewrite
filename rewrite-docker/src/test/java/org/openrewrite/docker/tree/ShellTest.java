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

class ShellTest implements RewriteTest {

    @Test
    void shellInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              SHELL ["/bin/bash", "-c"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Shell shell = (Docker.Shell) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(shell.getArguments()).hasSize(2);
            })
          )
        );
    }

    @Test
    void shellWithSpacesInJsonArray() {
        // SHELL with spaces inside JSON array
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              SHELL [ "/bin/bash", "-c" ]
              """
          )
        );
    }

    @Test
    void shellWithMultipleSpacesBeforeBracket() {
        // SHELL with multiple spaces between keyword and opening bracket
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              SHELL  ["/bin/bash", "-c"]
              """
          )
        );
    }

    @Test
    void shellWithTabBeforeBracket() {
        // SHELL with tab between keyword and opening bracket
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              SHELL\t["/bin/bash", "-c"]
              """
          )
        );
    }

    @Test
    void shellWithNoSpaceBeforeBracket() {
        // SHELL with no space between keyword and opening bracket
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              SHELL["/bin/bash", "-c"]
              """
          )
        );
    }
}
