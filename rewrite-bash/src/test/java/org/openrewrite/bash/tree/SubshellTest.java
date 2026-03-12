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

class SubshellTest implements RewriteTest {

    @Test
    void subshell() {
        rewriteRun(
          bash(
            "(cd /tmp && ls)\n"
          )
        );
    }

    @Test
    void braceGroup() {
        rewriteRun(
          bash(
            "{ echo hello; echo world; }\n"
          )
        );
    }

    @Test
    void subshellInPipeline() {
        rewriteRun(
          bash(
            "(echo a; echo b) | sort\n"
          )
        );
    }

    @Test
    void deeplyNestedSubshells() {
        rewriteRun(
          bash(
            "((((( cmd 2>&1; echo $? >&3) | cat >&4) 3>&1) | (read xs; exit $xs)) 4>>log &&\necho ok) ||\necho fail\n"
          )
        );
    }
}
