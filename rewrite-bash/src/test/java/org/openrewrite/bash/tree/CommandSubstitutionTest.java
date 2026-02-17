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

class CommandSubstitutionTest implements RewriteTest {

    @Test
    void dollarParen() {
        rewriteRun(
          bash(
            "echo $(whoami)\n"
          )
        );
    }

    @Test
    void dollarParenAssignment() {
        rewriteRun(
          bash(
            "VAR=$(date)\n"
          )
        );
    }

    @Test
    void backtick() {
        rewriteRun(
          bash(
            "VAR=`date`\n"
          )
        );
    }

    @Test
    void nested() {
        rewriteRun(
          bash(
            "VAR=$(echo $(hostname))\n"
          )
        );
    }

    @Test
    void inDoubleQuotes() {
        rewriteRun(
          bash(
            "echo \"today: $(date +%Y-%m-%d)\"\n"
          )
        );
    }

    @Test
    void withPipeline() {
        rewriteRun(
          bash(
            "VAR=$(echo hello | tr 'a-z' 'A-Z')\n"
          )
        );
    }

    @Test
    void backtickInForLoop() {
        rewriteRun(
          bash(
            "for ROW in `echo \"$data\" | jq -r '.values[]'`; do\n  echo $ROW\ndone\n"
          )
        );
    }

    @Test
    void backtickInDoubleQuotes() {
        rewriteRun(
          bash(
            "echo \"result: `date`\"\n"
          )
        );
    }

    @Test
    void dockerComposeWithPipeAndRedirect() {
        rewriteRun(
          bash(
            "server_id=$(docker-compose exec -T db psql -U user -d db -t -c \"SELECT 1;\" 2>/dev/null | tr -d ' \\n')\n"
          )
        );
    }

    @Test
    void awkFieldSeparator() {
        rewriteRun(
          bash(
            "DATA=$(awk -F',' -v col=\"$COL\" 'NR > 1 { print $col }' \"$FILE\")\n"
          )
        );
    }

    @Test
    void perlOneLiner() {
        rewriteRun(
          bash(
            "encoded=\"$(perl -e 'print lc($ARGV[0])' \"$name\")\"\n"
          )
        );
    }

    @Test
    void commandSubWithPipelineCut() {
        rewriteRun(
          bash(
            "if [ $(head -n 1 \"$TEST\" | cut -c -2) = \"#!\" ]\nthen\n  echo found\nfi\n"
          )
        );
    }

    @Test
    void deeplyNestedCommandSub() {
        rewriteRun(
          bash(
            "hash=\"$(nix hash convert \"$(nix-prefetch-url \"$url\")\")\"\n"
          )
        );
    }

    @Test
    void deeplyNestedCommandSubMultiLine() {
        rewriteRun(
          bash(
            "hash=\"$(\nnix \"$(\ncmd \"url\"\n)\"\n)\"\n"
          )
        );
    }

    @Test
    void commandSubThatLooksLikeArithmetic() {
        rewriteRun(
          bash(
            "ERRMSG=$((echo $$ > cgroup.procs) |& cat)\n"
          )
        );
    }

    @Test
    void nestedBacktick() {
        rewriteRun(
          bash(
            "driver=`basename \\`realpath /sys/class/net/eth0/device/driver\\``\n"
          )
        );
    }

    @Test
    void arrayExpansionInCommandSub() {
        rewriteRun(
          bash(
            "sizes=$(filesizes \"$FBZIMAGE\" \"${FDINITRDS[@]}\" \"$efishell\")\n"
          )
        );
    }

    @Test
    void hereStringInCommandSub() {
        rewriteRun(
          bash(
            "OS=$(cut -d'/' -f1 <<< \"${PLATFORM}\")\n"
          )
        );
    }

    @Test
    void localWithCommandSub() {
        rewriteRun(
          bash(
            "f() {\n  local dir=$(find /tmp -type d)\n  echo $dir\n}\n"
          )
        );
    }
}
