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
package org.openrewrite.bash.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.bash.Assertions.bash;

class CommandListTest implements RewriteTest {

    @Test
    void andList() {
        rewriteRun(
          bash(
            "cmd1 && cmd2\n"
          )
        );
    }

    @Test
    void orList() {
        rewriteRun(
          bash(
            "cmd1 || cmd2\n"
          )
        );
    }

    @Test
    void andWithMkdir() {
        rewriteRun(
          bash(
            "mkdir -p /tmp/dir && echo created\n"
          )
        );
    }

    @Test
    void orWithMissing() {
        rewriteRun(
          bash(
            "test -f file || echo missing\n"
          )
        );
    }

    @Test
    void andOrChain() {
        rewriteRun(
          bash(
            "cmd1 && cmd2 || cmd3\n"
          )
        );
    }

    @Test
    void orWithEmptyFallback() {
        rewriteRun(
          bash(
            "VAR=$(command || echo \"\")\n"
          )
        );
    }

    @Test
    void trueOrFalse() {
        rewriteRun(
          bash(
            "command || true\n"
          )
        );
    }

    @Test
    void conditionalAndBraceGroup() {
        rewriteRun(
          bash(
            "[[ \"$x\" == \"y\" ]] && { echo yes; exit 0; }\n"
          )
        );
    }

    @Test
    void andListInBackground() {
        rewriteRun(
          bash(
            "cmd1 && cmd2 &\n"
          )
        );
    }

    @Test
    void orListInBackground() {
        rewriteRun(
          bash(
            "cmd1 || cmd2 &\n"
          )
        );
    }

    @Test
    void andOrChainInBackground() {
        rewriteRun(
          bash(
            "cmd1 && cmd2 || cmd3 &\n"
          )
        );
    }
}
