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

class CommandTest implements RewriteTest {

    @Test
    void echoHello() {
        rewriteRun(
          bash(
            "echo hello\n"
          )
        );
    }

    @Test
    void commandWithFlags() {
        rewriteRun(
          bash(
            "ls -la /tmp\n"
          )
        );
    }

    @Test
    void commandWithLongFlag() {
        rewriteRun(
          bash(
            "curl --silent --fail http://example.com\n"
          )
        );
    }

    @Test
    void multipleCommandsOnSeparateLines() {
        rewriteRun(
          bash(
            "echo one\necho two\necho three\n"
          )
        );
    }

    @Test
    void semicolonSeparatedCommands() {
        rewriteRun(
          bash(
            "echo one; echo two; echo three\n"
          )
        );
    }

    @Test
    void backgroundCommand() {
        rewriteRun(
          bash(
            "sleep 10 &\n"
          )
        );
    }

    @Test
    void pipelineInBackground() {
        rewriteRun(
          bash(
            "echo ondemand | sudo tee /sys/policy &\n"
          )
        );
    }

    @Test
    void backgroundFollowedByCommand() {
        rewriteRun(
          bash(
            "sleep 10 & echo started\n"
          )
        );
    }

    @Test
    void shebang() {
        rewriteRun(
          bash(
            "#!/bin/bash\necho hello\n"
          )
        );
    }

    @Test
    void shebangWithFlags() {
        rewriteRun(
          bash(
            "#!/usr/bin/env bash\necho hello\n"
          )
        );
    }

    @Test
    void comments() {
        rewriteRun(
          bash(
            "# This is a comment\necho hello # inline comment\n"
          )
        );
    }

    @Test
    void emptyScript() {
        rewriteRun(
          bash(
            "\n"
          )
        );
    }

    @Test
    void exitWithCode() {
        rewriteRun(
          bash(
            "exit 0\n"
          )
        );
    }

    @Test
    void returnWithValue() {
        rewriteRun(
          bash(
            "return 1\n"
          )
        );
    }

    @Test
    void colonNoop() {
        rewriteRun(
          bash(
            ": \"no-op command\"\n"
          )
        );
    }

    @Test
    void sourceCommand() {
        rewriteRun(
          bash(
            "source ./lib/utils.sh\n"
          )
        );
    }

    @Test
    void dotSource() {
        rewriteRun(
          bash(
            ". ./lib/utils.sh\n"
          )
        );
    }

    @Test
    void trapExit() {
        rewriteRun(
          bash(
            "trap cleanup EXIT\n"
          )
        );
    }

    @Test
    void trapMultipleSignals() {
        rewriteRun(
          bash(
            "trap 'rm -f $tmpfile' EXIT INT TERM\n"
          )
        );
    }

    @Test
    void trapWithQuotedCommand() {
        rewriteRun(
          bash(
            "trap 'echo caught signal' SIGINT SIGTERM\n"
          )
        );
    }

    @Test
    void curlWithMultipleFlags() {
        rewriteRun(
          bash(
            "response=$(curl --silent -H \"Content-Type: application/json\" -H \"Authorization: Bearer $TOKEN\" \"$url\")\n"
          )
        );
    }

    @Test
    void completeSmallScript() {
        rewriteRun(
          bash(
            """
            #!/bin/bash
            # Simple build script
            set -euo pipefail

            readonly VERSION="${1:-latest}"
            readonly OUTPUT_DIR="build/$VERSION"

            cleanup() {
                rm -rf "$OUTPUT_DIR/tmp"
            }
            trap cleanup EXIT

            mkdir -p "$OUTPUT_DIR"

            if [[ "$VERSION" == "latest" ]]; then
                echo "Building latest..."
            else
                echo "Building version $VERSION..."
            fi

            for f in src/*.sh; do
                cp "$f" "$OUTPUT_DIR/"
            done

            echo "Build complete: $(ls "$OUTPUT_DIR" | wc -l) files"
            """
          )
        );
    }

    @Test
    void echoWithEqualsSign() {
        rewriteRun(
          bash(
            "echo Log lines = $log0\n"
          )
        );
    }

    @Test
    void commandWithGlobAndBackground() {
        rewriteRun(
          bash(
            "echo ondemand | sudo tee /sys/policy*/scaling_governor &\n"
          )
        );
    }

    @Test
    void jqComplexArgs() {
        rewriteRun(
          bash(
            "jq -n --arg v \"$VERSION\" '{version: $v}' > out.json\n"
          )
        );
    }

    @Test
    void escapedParensInArgs() {
        rewriteRun(
          bash(
            "find -L /usr/share \\( -name \"*.bin\" -or -iname \"*.efi\" \\) -type f -print\n"
          )
        );
    }
}
