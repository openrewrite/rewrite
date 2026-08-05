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
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class FindTypesTest implements RewriteTest {

    @Test
    void findsParameterizedTypeUsages() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("typing.List", false)),
          python(
            """
              from typing import List

              def foo(items: List[str]) -> List[int]:
                  pass
              """,
            """
              from typing import List

              def foo(items: /*~~>*/List[str]) -> /*~~>*/List[int]:
                  pass
              """
          )
        );
    }

    @Test
    void findsUserDefinedGenericUsages() {
        // Unlike the aliased `typing` generics, a user-defined generic's parameterized type carries the
        // searched type itself and is marked as a whole, matching the Java rendering.
        rewriteRun(
          spec -> spec.recipe(new FindTypes("file.Box", false)),
          python(
            """
              from typing import Generic, TypeVar

              T = TypeVar("T")

              class Box(Generic[T]):
                  pass

              def foo(b: Box[str]) -> None:
                  pass
              """,
            """
              from typing import Generic, TypeVar

              T = TypeVar("T")

              class Box(Generic[T]):
                  pass

              def foo(b: /*~~>*/Box[str]) -> None:
                  pass
              """
          )
        );
    }
}
