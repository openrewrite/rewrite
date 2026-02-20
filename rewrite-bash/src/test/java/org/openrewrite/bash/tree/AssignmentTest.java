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

class AssignmentTest implements RewriteTest {

    @Test
    void simpleAssignment() {
        rewriteRun(
          bash(
            "VAR=value\n"
          )
        );
    }

    @Test
    void quotedAssignment() {
        rewriteRun(
          bash(
            "VAR=\"hello world\"\n"
          )
        );
    }

    @Test
    void emptyAssignment() {
        rewriteRun(
          bash(
            "VAR=\n"
          )
        );
    }

    @Test
    void assignmentWithCommandSubstitution() {
        rewriteRun(
          bash(
            "VAR=$(date +%Y%m%d)\n"
          )
        );
    }

    @Test
    void plusEqualsAppend() {
        rewriteRun(
          bash(
            "PATH+=\":/usr/local/bin\"\n"
          )
        );
    }

    @Test
    void exportWithAssignment() {
        rewriteRun(
          bash(
            "export VAR=value\n"
          )
        );
    }

    @Test
    void exportWithCommandSubstitution() {
        rewriteRun(
          bash(
            "export INSTANCE_ID=$(curl -sf http://example.com 2>/dev/null || echo \"localhost\")\n"
          )
        );
    }

    @Test
    void multipleLocalAssignments() {
        rewriteRun(
          bash(
            "local first_sum=0 last_sum=0\n"
          )
        );
    }

    @Test
    void readonlyAssignment() {
        rewriteRun(
          bash(
            "readonly MAX_RETRIES=3\n"
          )
        );
    }

    @Test
    void assignmentWithCommand() {
        rewriteRun(
          bash(
            "VAR=value echo hello\n"
          )
        );
    }
}
