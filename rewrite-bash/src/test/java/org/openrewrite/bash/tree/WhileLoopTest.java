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

class WhileLoopTest implements RewriteTest {

    @Test
    void simpleWhile() {
        rewriteRun(
          bash(
            "while true; do\n  echo loop\ndone\n"
          )
        );
    }

    @Test
    void whileCondition() {
        rewriteRun(
          bash(
            "while [ $attempt -le $max ]; do\n  sleep 1\n  attempt=$((attempt + 1))\ndone\n"
          )
        );
    }

    @Test
    void whileRead() {
        rewriteRun(
          bash(
            "while IFS= read -r line; do\n  echo \"$line\"\ndone < input.txt\n"
          )
        );
    }

    @Test
    void whileReadWithOr() {
        rewriteRun(
          bash(
            "while IFS= read -r line || [[ -n \"$line\" ]]; do\n  echo \"$line\"\ndone < input.txt\n"
          )
        );
    }

    @Test
    void whileWithPipeInput() {
        rewriteRun(
          bash(
            "echo \"data\" | while IFS= read -r line; do\n  echo \"$line\"\ndone\n"
          )
        );
    }

    @Test
    void untilLoop() {
        rewriteRun(
          bash(
            "until [ -f /tmp/ready ]; do\n  sleep 1\ndone\n"
          )
        );
    }

    @Test
    void whileWithDoubleBracketString() {
        rewriteRun(
          bash(
            "while [ \"$last_page\" != \"true\" ] ; do\n  echo working\ndone\n"
          )
        );
    }

    @Test
    void whileReadWithCustomIFS() {
        rewriteRun(
          bash(
            "while IFS='|' read -r origin url; do\n  echo \"$origin $url\"\ndone\n"
          )
        );
    }

    @Test
    void whileReadArray() {
        rewriteRun(
          bash(
            "while IFS='.' read -ra parts <<< \"$version\"; do\n  echo \"${parts[0]}\"\ndone\n"
          )
        );
    }

    @Test
    void whileWithIfAndRedirection() {
        rewriteRun(
          bash(
            """
            while [ $attempt -le $max_attempts ]; do
                if curl -s -f http://localhost:7990/status > /dev/null 2>&1; then
                    return 0
                fi
                sleep 2
                attempt=$((attempt + 1))
            done
            """
          )
        );
    }

    @Test
    void whileReadWithCaseBody() {
        rewriteRun(
          bash(
            """
            while IFS= read -r line || [[ -n "$line" ]]; do
                case "$line" in
                    ''|'#'*) continue ;;
                esac
                echo "$line"
            done < "$CSV_FILE"
            """
          )
        );
    }

    @Test
    void whileReadFromFile() {
        rewriteRun(
          bash(
            "while IFS= read -r line; do\n  echo \"$line\"\ndone < /tmp/input\n"
          )
        );
    }
}
