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

class CaseStatementTest implements RewriteTest {

    @Test
    void simpleCase() {
        rewriteRun(
          bash(
            "case $x in\n  a)\n    echo a\n    ;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithMultipleArms() {
        rewriteRun(
          bash(
            "case $opt in\n  a) echo a;;\n  b) echo b;;\n  *) echo default;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithPatternAlternation() {
        rewriteRun(
          bash(
            "case $1 in\n  -o|--output) OUTPUT=$2; shift;;\n  -v|--verbose) VERBOSE=true;;\n  *) echo unknown;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithQuotedPatterns() {
        rewriteRun(
          bash(
            "case \"$response\" in\n  \"yes\"|\"y\") echo ok;;\n  \"no\"|\"n\") echo nope;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithGlobPattern() {
        rewriteRun(
          bash(
            "case \"$file\" in\n  *.tar.gz) tar xzf \"$file\";;\n  *.zip) unzip \"$file\";;\nesac\n"
          )
        );
    }

    @Test
    void caseWithEmptyBody() {
        rewriteRun(
          bash(
            "case $1 in\n  '') echo empty;;\n  ' '*'#'*) continue;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithMultipleStatementsPerArm() {
        rewriteRun(
          bash(
            "case $1 in\n  start)\n    echo starting\n    run_app\n    ;;\n  stop)\n    echo stopping\n    kill_app\n    ;;\nesac\n"
          )
        );
    }

    @Test
    void whileShiftCase() {
        rewriteRun(
          bash(
            "while [[ $# -gt 0 ]]; do\n  case $1 in\n    -h|--help) usage; exit 0;;\n    *) break;;\n  esac\n  shift\ndone\n"
          )
        );
    }

    @Test
    void argParseShiftCase() {
        rewriteRun(
          bash(
            """
            while [[ $# -gt 0 ]]; do
              case $1 in
                -o|--output-dir)
                  OUTPUT_DIR="$2"
                  shift
                  ;;
                -v|--verbose)
                  VERBOSE=true
                  ;;
                -h|--help)
                  usage
                  exit 0
                  ;;
                *)
                  echo "Unknown option: $1"
                  exit 1
                  ;;
              esac
              shift
            done
            """
          )
        );
    }

    @Test
    void casePatternWithSpaceBeforeParen() {
        rewriteRun(
          bash(
            "case $1 in\n  a )\n    echo a\n    ;;\nesac\n"
          )
        );
    }

    @Test
    void caseWithLeadingParen() {
        rewriteRun(
          bash(
            "case \"$1\" in\n  (yes) echo y ;;\n  (no) echo n ;;\nesac\n"
          )
        );
    }

    @Test
    void complexCaseWithMultiLineBody() {
        rewriteRun(
          bash(
            """
            case "$1" in
              -h|--help)
                echo "Usage: script [options]"
                ;;
              -v|--verbose)
                VERBOSE=1
                ;;
              *)
                echo "Unknown option: $1"
                ;;
            esac
            """
          )
        );
    }
}
