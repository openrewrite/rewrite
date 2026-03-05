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

class IfStatementTest implements RewriteTest {

    @Test
    void simpleIf() {
        rewriteRun(
          bash(
            "if true; then\n  echo yes\nfi\n"
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          bash(
            "if true; then\n  echo yes\nelse\n  echo no\nfi\n"
          )
        );
    }

    @Test
    void ifWithBracketCondition() {
        rewriteRun(
          bash(
            "if [ -z \"$VAR\" ]; then\n  echo empty\nfi\n"
          )
        );
    }

    @Test
    void ifElseWithFileTest() {
        rewriteRun(
          bash(
            "if [ -f /tmp/file ]; then\n  echo exists\nelse\n  echo missing\nfi\n"
          )
        );
    }

    @Test
    void ifElifElse() {
        rewriteRun(
          bash(
            "if [ \"$x\" = \"a\" ]; then\n  echo a\nelif [ \"$x\" = \"b\" ]; then\n  echo b\nelse\n  echo other\nfi\n"
          )
        );
    }

    @Test
    void nestedIf() {
        rewriteRun(
          bash(
            "if [ -f file ]; then\n  if [ -r file ]; then\n    echo readable\n  fi\nfi\n"
          )
        );
    }

    @Test
    void singleBracketWithBang() {
        rewriteRun(
          bash(
            "if [ ! -d /tmp/dir ]; then\n  mkdir /tmp/dir\nfi\n"
          )
        );
    }

    @Test
    void compoundConditionWithAnd() {
        rewriteRun(
          bash(
            "if [ -n \"$a\" ] && [ -n \"$b\" ]; then\n  echo both\nfi\n"
          )
        );
    }

    @Test
    void compoundConditionWithOr() {
        rewriteRun(
          bash(
            "if [ -z \"$a\" ] || [ -z \"$b\" ]; then\n  echo missing\nfi\n"
          )
        );
    }

    @Test
    void ifWithArithmeticCondition() {
        rewriteRun(
          bash(
            "if (( count > 10 )); then\n  echo many\nfi\n"
          )
        );
    }

    @Test
    void ifWithCommandCondition() {
        rewriteRun(
          bash(
            "if curl -sf http://localhost:8080/health > /dev/null 2>&1; then\n  echo up\nfi\n"
          )
        );
    }

    @Test
    void elifWithArithmetic() {
        rewriteRun(
          bash(
            "if (( avg > 500 )); then\n  echo high\nelif (( avg > 200 )); then\n  echo medium\nelse\n  echo low\nfi\n"
          )
        );
    }

    @Test
    void grepExtendedRegex() {
        rewriteRun(
          bash(
            "if echo \"$OUTPUT\" | grep -qiE \"authentication|credential|401|403\"; then\n  echo \"auth failed\"\nfi\n"
          )
        );
    }

    @Test
    void multiLineIfWithDoubleBracket() {
        rewriteRun(
          bash(
            """
            if [[ "$REF" =~ "test/" ||
                  "$REF" =~ "actions/" ||
                  "$MSG" =~ "docs:"
                ]] ; then
                echo "skip"
                exit 0
            else
                echo "build"
                exit 1
            fi
            """
          )
        );
    }

    @Test
    void ifElseWithStringComparison() {
        rewriteRun(
          bash(
            """
            if [ -z "$CLONE_PROTOCOL" ] || [ "$CLONE_PROTOCOL" != "ssh" ]; then
                CLONE_PROTOCOL=http
            fi

            if [ "$CLONE_PROTOCOL" = "ssh" ]; then
                clone_url="git@${host}:${project}/${repo}.git"
            else
                clone_url="https://${host}/${project}/${repo}.git"
            fi
            """
          )
        );
    }

    @Test
    void ifWithNewlineBeforeThen() {
        rewriteRun(
          bash(
            "if [ \"$log0\" = \"$log1\" ];\nthen\n\techo fail\n\texit 1\nelse\n\techo pass\nfi\n"
          )
        );
    }

    @Test
    void ifWithGlobTest() {
        rewriteRun(
          bash(
            "if test -f $out/lib/ld.so.?; then\n  echo found\nfi\n"
          )
        );
    }

    @Test
    void nestedIfInFunction() {
        rewriteRun(
          bash(
            """
            check() {
              if [[ -z "$1" ]]; then
                echo "empty"
                return 1
              fi
              echo "ok"
            }
            """
          )
        );
    }

    @Test
    void ifWithSymlinkContinue() {
        rewriteRun(
          bash(
            "for i in /usr/bin/*; do\n    if [ -L \"$i\" ]; then continue; fi\n    echo \"$i\"\ndone\n"
          )
        );
    }
}
