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

class ConditionalExpressionTest implements RewriteTest {

    @Test
    void doubleBracket() {
        rewriteRun(
          bash(
            "[[ -f file.txt ]]\n"
          )
        );
    }

    @Test
    void stringEquals() {
        rewriteRun(
          bash(
            "[[ \"$VAR\" == \"value\" ]]\n"
          )
        );
    }

    @Test
    void stringNotEquals() {
        rewriteRun(
          bash(
            "[[ \"$VAR\" != \"value\" ]]\n"
          )
        );
    }

    @Test
    void nonEmpty() {
        rewriteRun(
          bash(
            "[[ -n \"$VAR\" ]]\n"
          )
        );
    }

    @Test
    void regexMatch() {
        rewriteRun(
          bash(
            "[[ \"$x\" =~ ^[2-3] ]]\n"
          )
        );
    }

    @Test
    void regexWithAlternation() {
        rewriteRun(
          bash(
            "[[ \"$FILE_PERMS\" =~ ^(400|440|444|000)$ ]]\n"
          )
        );
    }

    @Test
    void regexWithPosixClass() {
        rewriteRun(
          bash(
            "[[ \"$line\" =~ ^[[:space:]]*[[:graph:]] ]]\n"
          )
        );
    }

    @Test
    void regexWithBackslashGroups() {
        rewriteRun(
          bash(
            "[[ \"$line\" =~ \\([0-9]+\\)$ ]]\n"
          )
        );
    }

    @Test
    void logicalAndInBrackets() {
        rewriteRun(
          bash(
            "[[ -n \"$a\" && -n \"$b\" ]]\n"
          )
        );
    }

    @Test
    void logicalOrInBrackets() {
        rewriteRun(
          bash(
            "[[ -z \"$a\" || -z \"$b\" ]]\n"
          )
        );
    }

    @Test
    void regexWithBraceExpansionPattern() {
        rewriteRun(
          bash(
            "[[ \"$id\" =~ ^[A-Z0-9]{4}-[A-Z0-9]{4}$ ]]\n"
          )
        );
    }

    @Test
    void regexWithCharacterClassInBrackets() {
        rewriteRun(
          bash(
            "[[ \"$password\" =~ [/:@\\ %?+] ]]\n"
          )
        );
    }

    @Test
    void patternMatch() {
        rewriteRun(
          bash(
            "[[ \"$file\" == *.sh ]]\n"
          )
        );
    }

    @Test
    void hashInRegexQuoted() {
        rewriteRun(
          bash(
            "if [[ \"$first_line\" =~ \"^#+\" ]]; then\n  echo heading\nfi\n"
          )
        );
    }
}
