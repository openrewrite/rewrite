/*
 * Copyright 2025 the original author or authors.
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

class PrintStatementTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "print 42",
      "print  42",
      "print 1, 2, 3, 4",
      "print 1 , 2 , 3 , 4",
    })
    void printStatement(@Language("py") String code) {
        rewriteRun(python(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "print 1, 2, 3,",
      "print 1 , 2 , 3 ,",
    })
    void printStatementTrailingComma(@Language("py") String code) {
        rewriteRun(python(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "print >>stderr, 'error'",
      "print >>  stderr, 'error'",
      "print >> stderr , 'error'",
    })
    void printStatementWithDestination(@Language("py") String code) {
        rewriteRun(python(code));
    }
}
