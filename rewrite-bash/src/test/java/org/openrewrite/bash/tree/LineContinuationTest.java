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

class LineContinuationTest implements RewriteTest {

    @Test
    void backslashContinuation() {
        rewriteRun(
          bash(
            "echo hello \\\n  world\n"
          )
        );
    }

    @Test
    void multiLineFindExec() {
        rewriteRun(
          bash(
            "find . \\\n    -type f -name '*.sh' \\\n    -exec cp {} dest/ \\;\n"
          )
        );
    }

    @Test
    void multiLineCommand() {
        rewriteRun(
          bash(
            "curl \\\n  --silent \\\n  --fail \\\n  http://example.com\n"
          )
        );
    }

    @Test
    void emptyBraces() {
        rewriteRun(
          bash(
            "find . -exec cp {} /tmp \\;\n"
          )
        );
    }
}
