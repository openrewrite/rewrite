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

class QuotingTest implements RewriteTest {

    @Test
    void singleQuotes() {
        rewriteRun(
          bash(
            "echo 'hello world'\n"
          )
        );
    }

    @Test
    void doubleQuotes() {
        rewriteRun(
          bash(
            "echo \"hello world\"\n"
          )
        );
    }

    @Test
    void doubleQuotesWithVariable() {
        rewriteRun(
          bash(
            "echo \"Hello, $NAME\"\n"
          )
        );
    }

    @Test
    void doubleQuotesWithBraceExpansion() {
        rewriteRun(
          bash(
            "echo \"${VAR}\"\n"
          )
        );
    }

    @Test
    void doubleQuotesWithCommandSubstitution() {
        rewriteRun(
          bash(
            "echo \"Today is $(date)\"\n"
          )
        );
    }

    @Test
    void doubleQuotesWithArithmetic() {
        rewriteRun(
          bash(
            "echo \"Result: $((2 + 2))\"\n"
          )
        );
    }

    @Test
    void dollarSingleQuotedString() {
        rewriteRun(
          bash(
            "echo $'line1\\nline2\\ttab'\n"
          )
        );
    }

    @Test
    void bareDollarInDoubleQuotes() {
        rewriteRun(
          bash(
            "grep -v \"^$\"\n"
          )
        );
    }

    @Test
    void bareDollarUnquoted() {
        rewriteRun(
          bash(
            "[[ \"$x\" =~ ^(yes|no)$ ]]\n"
          )
        );
    }

    @Test
    void escapedCharactersInDoubleQuotes() {
        rewriteRun(
          bash(
            "echo \"path: \\$HOME and \\\"quoted\\\"\"\n"
          )
        );
    }

    @Test
    void adjacentQuotedStrings() {
        rewriteRun(
          bash(
            "echo \"hello\"'world'\"again\"\n"
          )
        );
    }

    @Test
    void emptyStrings() {
        rewriteRun(
          bash(
            "echo \"\" '' \"$VAR\"\n"
          )
        );
    }

    @Test
    void emojiInString() {
        rewriteRun(
          bash(
            "echo \"\uD83D\uDED1 Error: something failed\"\n"
          )
        );
    }

    @Test
    void emojiInCommentAndString() {
        rewriteRun(
          bash(
            "# \uD83D\uDE80 Deploy script\necho \"\u2705 Success\"\necho \"\u274C Failure\"\n"
          )
        );
    }

    @Test
    void dollarSingleQuoteEscapes() {
        rewriteRun(
          bash(
            "echo $'line1\\nline2\\ttab'\n"
          )
        );
    }

    @Test
    void evalWithComplexQuoting() {
        rewriteRun(
          bash(
            "eval \"$(printf '%s=(${%s[@]+\"${%s[@]}\"})' dst src src)\"\n"
          )
        );
    }

    @Test
    void regexEscapesInDoubleQuotes() {
        rewriteRun(
          bash(
            "export REGEX=\"^v(0|[1-9][0-9]*)\\\\.(0|[1-9][0-9]*)\\\\.(0|[1-9][0-9]*)(-([a-zA-Z0-9]+)\\\\.(0|[1-9][0-9]*))?$\"\n"
          )
        );
    }
}
