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

class ForLoopTest implements RewriteTest {

    @Test
    void simpleFor() {
        rewriteRun(
          bash(
            "for i in a b c; do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void forInList() {
        rewriteRun(
          bash(
            "for i in 1 2 3; do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void forInVariable() {
        rewriteRun(
          bash(
            "for item in $items; do\n  echo $item\ndone\n"
          )
        );
    }

    @Test
    void forInGlob() {
        rewriteRun(
          bash(
            "for f in *.sh; do\n  echo $f\ndone\n"
          )
        );
    }

    @Test
    void forInBraceExpansion() {
        rewriteRun(
          bash(
            "for i in {1..10}; do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void forInBraceExpansionRange() {
        rewriteRun(
          bash(
            "for i in {0..4}; do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void forInCommandSubstitution() {
        rewriteRun(
          bash(
            "for f in $(ls); do\n  echo $f\ndone\n"
          )
        );
    }

    @Test
    void forInBacktick() {
        rewriteRun(
          bash(
            "for ROW in `echo \"$response\" | jq -r '.values[]'`; do\n  echo $ROW\ndone\n"
          )
        );
    }

    @Test
    void forInQuotedArrayExpansion() {
        rewriteRun(
          bash(
            "for l in \"${latencies[@]}\"; do\n  echo $l\ndone\n"
          )
        );
    }

    @Test
    void cStyleForLoop() {
        rewriteRun(
          bash(
            "for ((i=0; i<10; i++)); do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void cStyleForWithSpaces() {
        rewriteRun(
          bash(
            "for ((i = 1; i <= MAX; i++)); do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void forLoopInBackground() {
        rewriteRun(
          bash(
            "for i in 1 2 3; do\n  echo $i\ndone &\n"
          )
        );
    }

    @Test
    void selectStatement() {
        rewriteRun(
          bash(
            "select opt in a b c; do\n  echo \"$opt\"\n  break\ndone\n"
          )
        );
    }

    @Test
    void braceRangeInsideIfBody() {
        rewriteRun(
          bash(
            """
            if [[ -d "${dir}" ]]; then
              for i in {1..10}; do
                if try_restore; then
                  break
                fi
              done
            fi
            """
          )
        );
    }

    @Test
    void backtickInForLoop() {
        rewriteRun(
          bash(
            "for i in `seq 1 10`; do\n  echo $i\ndone\n"
          )
        );
    }
}
