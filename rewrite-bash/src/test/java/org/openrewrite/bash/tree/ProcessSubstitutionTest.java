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

class ProcessSubstitutionTest implements RewriteTest {

    @Test
    void inputProcessSubstitution() {
        rewriteRun(
          bash(
            "diff <(sort file1) <(sort file2)\n"
          )
        );
    }

    @Test
    void outputProcessSubstitution() {
        rewriteRun(
          bash(
            "tee >(grep error > errors.log) >(grep warn > warnings.log)\n"
          )
        );
    }

    @Test
    void processSubstitutionAsInput() {
        rewriteRun(
          bash(
            "while IFS= read -r line; do\n  echo \"$line\"\ndone < <($cmd show 2>/dev/null)\n"
          )
        );
    }

    @Test
    void braceGroupWithProcessSubRedirection() {
        rewriteRun(
          bash(
            "{ read -r hash; read -r path; } < <(echo test)\n"
          )
        );
    }

    @Test
    void processSubWithSpaceBeforeClose() {
        rewriteRun(
          bash(
            "diff <( sort file1 ) <( sort file2 )\n"
          )
        );
    }
}
