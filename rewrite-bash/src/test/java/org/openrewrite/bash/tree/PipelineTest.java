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

class PipelineTest implements RewriteTest {

    @Test
    void simplePipeline() {
        rewriteRun(
          bash(
            "echo hello | grep h\n"
          )
        );
    }

    @Test
    void multiStagePipeline() {
        rewriteRun(
          bash(
            "cat file | grep pattern | sort | uniq -c | head -10\n"
          )
        );
    }

    @Test
    void bangPipeline() {
        rewriteRun(
          bash(
            "if ! echo \"$data\" | grep -q pattern; then\n  echo not found\nfi\n"
          )
        );
    }

    @Test
    void bangSingleCommand() {
        rewriteRun(
          bash(
            "if ! check_command mod; then\n  echo missing\nfi\n"
          )
        );
    }

    @Test
    void bangInCommandList() {
        rewriteRun(
          bash(
            "if echo \"$x\" | grep -q ok || ! echo \"$y\" | grep -q fail; then\n  echo done\nfi\n"
          )
        );
    }

    @Test
    void pipeWithStderrRedirect() {
        rewriteRun(
          bash(
            "cmd 2>&1 | tee log.txt\n"
          )
        );
    }

    @Test
    void jqPipeline() {
        rewriteRun(
          bash(
            "last_page=$(echo \"$response\" | jq '. | .isLastPage')\n"
          )
        );
    }

    @Test
    void sedSubstitution() {
        rewriteRun(
          bash(
            "value=$(echo \"$clean\" | sed 's/^[[:space:]]*//' | grep -v \"^$\" || echo \"\")\n"
          )
        );
    }

    @Test
    void sedWithAlternateDelimiter() {
        rewriteRun(
          bash(
            "host=$(echo \"$url\" | sed 's|.*://||' | sed 's|.*@||' | cut -d/ -f1)\n"
          )
        );
    }
}
