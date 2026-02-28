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

class RedirectionTest implements RewriteTest {

    @Test
    void outputRedirect() {
        rewriteRun(
          bash(
            "echo hello > file.txt\n"
          )
        );
    }

    @Test
    void appendToFile() {
        rewriteRun(
          bash(
            "echo hello >> file.txt\n"
          )
        );
    }

    @Test
    void inputFromFile() {
        rewriteRun(
          bash(
            "sort < input.txt\n"
          )
        );
    }

    @Test
    void stderrToDevNull() {
        rewriteRun(
          bash(
            "command 2>/dev/null\n"
          )
        );
    }

    @Test
    void stderrToStdout() {
        rewriteRun(
          bash(
            "command 2>&1\n"
          )
        );
    }

    @Test
    void stdoutToStderr() {
        rewriteRun(
          bash(
            "echo error 1>&2\n"
          )
        );
    }

    @Test
    void bothToDevNull() {
        rewriteRun(
          bash(
            "command > /dev/null 2>&1\n"
          )
        );
    }

    @Test
    void clobber() {
        rewriteRun(
          bash(
            "echo data >| file.txt\n"
          )
        );
    }

    @Test
    void hereString() {
        rewriteRun(
          bash(
            "IFS=', ' read -r a b <<< \"$ROW\"\n"
          )
        );
    }

    @Test
    void heredoc() {
        rewriteRun(
          bash(
            "cat <<EOF\nhello world\nEOF\n"
          )
        );
    }

    @Test
    void heredocWithVariable() {
        rewriteRun(
          bash(
            "cat <<EOF\nHello, $NAME\nEOF\n"
          )
        );
    }

    @Test
    void heredocQuotedDelimiter() {
        rewriteRun(
          bash(
            "cat <<'EOF'\n$VAR is literal\nEOF\n"
          )
        );
    }

    @Test
    void heredocWithRedirection() {
        rewriteRun(
          bash(
            "cat > output.txt <<EOF\nhello\nEOF\n"
          )
        );
    }

    @Test
    void inputFromProcessSubstitution() {
        rewriteRun(
          bash(
            "while read -r line; do\n  echo \"$line\"\ndone < <(echo hello)\n"
          )
        );
    }

    @Test
    void heredocWithCommandAfter() {
        rewriteRun(
          bash(
            """
            cat > "$LOG" <<EOF
            #!/bin/bash
            # Generated on $(date)
            OUTPUT_DIR="prefix-\\$(date +%Y%m%d)"
            mkdir -p "\\$OUTPUT_DIR"
            EOF
            chmod +x "$LOG"
            """
          )
        );
    }

    @Test
    void heredocWithIndentedContent() {
        rewriteRun(
          bash(
            "cat <<EOF\n  line one\n    line two\n      line three\nEOF\n"
          )
        );
    }

    @Test
    void hereStringWithPrefixAssignment() {
        rewriteRun(
          bash(
            "IFS=: read -r sys platform <<< \"$pair\"\n"
          )
        );
    }

    @Test
    void inputRedirectBeforeCommand() {
        rewriteRun(
          bash(
            "< input.txt cat | head\n"
          )
        );
    }

    @Test
    void multipleHeredocsInScript() {
        rewriteRun(
          bash(
            "cat > /tmp/a <<EOF\nfirst\nEOF\ncat > /tmp/b <<EOF\nsecond\nEOF\n"
          )
        );
    }

    @Test
    void perfRecordWithRedirects() {
        rewriteRun(
          bash(
            "perf record -e arm_spe/period=65536/ -vvv -- prog > log 2>&1 &\n"
          )
        );
    }
}
