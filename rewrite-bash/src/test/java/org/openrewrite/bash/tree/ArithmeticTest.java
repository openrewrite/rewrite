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

class ArithmeticTest implements RewriteTest {

    @Test
    void arithmeticExpansion() {
        rewriteRun(
          bash(
            "echo $((2 + 2))\n"
          )
        );
    }

    @Test
    void arithmeticCommand() {
        rewriteRun(
          bash(
            "((count++))\n"
          )
        );
    }

    @Test
    void addAssign() {
        rewriteRun(
          bash(
            "((sum += val))\n"
          )
        );
    }

    @Test
    void comparison() {
        rewriteRun(
          bash(
            "if (( x > 0 )); then\n  echo positive\nfi\n"
          )
        );
    }

    @Test
    void logicalAndOr() {
        rewriteRun(
          bash(
            "if (( x > 0 && y > 0 )); then\n  echo both\nfi\n"
          )
        );
    }

    @Test
    void nestedParens() {
        rewriteRun(
          bash(
            "echo $(( (a + b) * c ))\n"
          )
        );
    }

    @Test
    void modulo() {
        rewriteRun(
          bash(
            "if [ $((attempt % 5)) -eq 0 ]; then\n  echo tick\nfi\n"
          )
        );
    }

    @Test
    void assignmentInArithmetic() {
        rewriteRun(
          bash(
            "((i = 0))\n"
          )
        );
    }

    @Test
    void ternaryInExpansion() {
        rewriteRun(
          bash(
            "echo $((x > 0 ? x : -x))\n"
          )
        );
    }

    @Test
    void arraySubscriptArithmetic() {
        rewriteRun(
          bash(
            "echo ${arr[$((idx + 1))]}\n"
          )
        );
    }

    @Test
    void arithmeticInsideCommandSub() {
        rewriteRun(
          bash(
            "echo $(($((a + b)) * 2))\n"
          )
        );
    }

    @Test
    void arithmeticWithSpecialVar() {
        rewriteRun(
          bash(
            "(( TEST_RESULT += $? ))\n"
          )
        );
    }

    @Test
    void tripleParenExpansion() {
        rewriteRun(
          bash(
            "echo $(((t1 - t0) / interval))\n"
          )
        );
    }

    @Test
    void tripleParenDivision() {
        rewriteRun(
          bash(
            "megs=$(((sizes + 2*1024 - 1)/(1024)))\n"
          )
        );
    }

    @Test
    void arithmeticBitwiseShift() {
        rewriteRun(
          bash(
            "result=$(( (1 << ($1 - 1)) - (1 << (64 - $2)) ))\n"
          )
        );
    }

    @Test
    void arithmeticInArrayIndex() {
        rewriteRun(
          bash(
            "arr[$((i+1))]=\"value\"\n"
          )
        );
    }
}
