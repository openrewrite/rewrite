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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class ComprehensionTest implements RewriteTest {

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "[x for x in xs if x]",
      "[ x for x in xs if x]",
      "[x  for x in xs if x]",
      "[x for  x in xs if x]",
      "[x for x  in xs if x]",
      "[x for x in  xs if x]",
      "[x for x in xs  if x]",
      "[x for x in xs if  x]",
      "[x for x in xs if x ]",
    })
    void listComprehension(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

    @Test
    void setComprehension() {
        rewriteRun(
          python("{x for x in xs if x}")
        );
    }

    @Test
    void dictComprehension() {
        rewriteRun(
          python("{x:x for x in xs if x}")
        );
    }

    @Test
    void generatorComprehension() {
        rewriteRun(
          python("(x for x in xs if x)")
        );
    }
}
