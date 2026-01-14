/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class BinaryTest implements RewriteTest {

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "1+2",
      "1+ 2",
      "1 +2",
    })
    void binaryOperatorSpacing(@Language("py") String expr) {
        rewriteRun(python(expr));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "+", "*", "/", "//", "**", "%", "@"})
    void arithmeticOperator(String op) {
        rewriteRun(python("1 %s 2".formatted(op)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "|", "^", ">>", "<<"})
    void bitwiseOperator(String op) {
        rewriteRun(python("1 %s 2".formatted(op)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"==", "!=", "<", ">", "<=", ">="})
    void comparisonOperator(String op) {
        rewriteRun(python("1 %s 2".formatted(op)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"or", "and", "is", "is not", "in", "not in"})
    void booleanOperator(String op) {
        rewriteRun(python("x %s y".formatted(op)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "in", " in", "in ",
      "not in", " not in", "not in ",
      "==", " ==", "== "
    })
    void magicMethodOperatorSpacing(String op) {
        rewriteRun(python("(x)%s(y)".formatted(op)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "not in", "not  in", " not  in", "not  in ",
      "is not", "is  not", " is  not", "is  not "
    })
    void operatorInternalSpacing(String op) {
        rewriteRun(python("(x)%s(y)".formatted(op)));
    }
}
