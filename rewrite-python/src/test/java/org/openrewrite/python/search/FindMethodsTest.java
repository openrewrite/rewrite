/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.python.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class FindMethodsTest implements RewriteTest {

    @Test
    void findsClassInstantiationAsConstructor() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("collections.OrderedDict <constructor>(..)", false)),
          python(
            """
              import collections
              x = collections.OrderedDict()
              """,
            """
              import collections
              x = /*~~>*/collections.OrderedDict()
              """
          )
        );
    }
}
