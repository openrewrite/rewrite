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
package org.openrewrite.python;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.SourceFile;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class PythonParserTest implements RewriteTest {

    @Test
    void parseString() {
        rewriteRun(
          python(
            """
              import sys
              print(sys.path)
              """,
            spec -> spec.afterRecipe(cu -> SoftAssertions.assertSoftly(softly -> {
                  softly.assertThat(cu).isInstanceOf(Py.CompilationUnit.class);
                  softly.assertThat(cu.getMarkers().getMarkers()).isEmpty();
              })
            )
          )
        );
    }

    @Test
    void parseStringWithParser() {
        SourceFile sf = PythonParser.builder().build()
          .parse(
            //language=python
            """
              import sys
              print(sys.path)
              """)
          .findFirst()
          .get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sf).isInstanceOf(Py.CompilationUnit.class);
            softly.assertThat(sf.getMarkers().getMarkers()).isEmpty();
        });
    }
}
