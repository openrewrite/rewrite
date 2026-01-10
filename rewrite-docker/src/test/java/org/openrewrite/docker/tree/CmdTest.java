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

class CmdTest implements RewriteTest {

    @Test
    void cmdShellForm() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              CMD nginx -g daemon off;
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Cmd cmd = (Docker.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Docker.ShellForm.class);
                Docker.ShellForm shellForm = (Docker.ShellForm) cmd.getCommandLine().getForm();
                assertThat(shellForm.getArguments()).hasSize(1);
            })
          )
        );
    }

    @Test
    void cmdExecForm() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              CMD ["nginx", "-g", "daemon off;"]
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Cmd cmd = (Docker.Cmd) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(cmd.getCommandLine().getForm()).isInstanceOf(Docker.ExecForm.class);
                Docker.ExecForm execForm = (Docker.ExecForm) cmd.getCommandLine().getForm();
                assertThat(execForm.getArguments()).hasSize(3);
            })
          )
        );
    }

    @Test
    void execFormWithSpacesBeforeClosingBracket() {
        // CMD with space before closing bracket: CMD [ "/bin/bash" ]
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              CMD [ "/bin/bash" ]
              """
          )
        );
    }
}
