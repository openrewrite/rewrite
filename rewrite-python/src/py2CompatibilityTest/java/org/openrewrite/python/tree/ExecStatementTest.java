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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@Disabled("Requires Py2ParserVisitor implementation")
class ExecStatementTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "exec code",
      "exec  code",
    })
    void execStatement(@Language("py") String code) {
        rewriteRun(python(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "exec code in globals_dict",
      "exec  code in globals_dict",
      "exec code  in globals_dict",
      "exec code in  globals_dict",
    })
    void execStatementWithGlobals(@Language("py") String code) {
        rewriteRun(python(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "exec code in globals_dict, locals_dict",
      "exec code in globals_dict , locals_dict",
      "exec code in globals_dict,  locals_dict",
    })
    void execStatementWithGlobalsAndLocals(@Language("py") String code) {
        rewriteRun(python(code));
    }
}
