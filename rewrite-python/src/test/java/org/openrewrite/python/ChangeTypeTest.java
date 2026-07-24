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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.java.ChangeType;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

/**
 * The Python upgrade recipes delegate PEP 585 type changes to Java's {@code ChangeType}, which must
 * reach imports through Python's import service so it emits {@code from a.b import C} rather than the
 * corrupting {@code import a.b.C} Java's {@code AddImport} would print (customer-requests#2858).
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class ChangeTypeTest implements RewriteTest {

    @Test
    void rewritesFromImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("typing.Iterable", "collections.abc.Iterable", true)),
          python(
            """
              from typing import Iterable

              def f(it: Iterable) -> None:
                  pass
              """,
            """
              from collections.abc import Iterable

              def f(it: Iterable) -> None:
                  pass
              """
          )
        );
    }

    @Test
    void keepsModuleDocstringFirst() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("typing.Iterable", "collections.abc.Iterable", true)),
          python(
            """
              \"""
              A module docstring.
              \"""
              from typing import Iterable

              def f(it: Iterable) -> None:
                  pass
              """,
            """
              \"""
              A module docstring.
              \"""
              from collections.abc import Iterable

              def f(it: Iterable) -> None:
                  pass
              """
          )
        );
    }

    @Test
    void keepsLeadingCommentAttachedToTheFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("typing.Iterable", "collections.abc.Iterable", true)),
          python(
            """
              # Copyright 2016 Someone
              from typing import Iterable

              def f(it: Iterable) -> None:
                  pass
              """,
            """
              # Copyright 2016 Someone
              from collections.abc import Iterable

              def f(it: Iterable) -> None:
                  pass
              """
          )
        );
    }

    @Test
    void mergesIntoAnExistingImportFromTheTargetModule() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("typing.Iterable", "collections.abc.Iterable", true)),
          python(
            """
              from collections.abc import Mapping
              from typing import Iterable

              def f(it: Iterable, m: Mapping) -> None:
                  pass
              """,
            """
              from collections.abc import Iterable, Mapping

              def f(it: Iterable, m: Mapping) -> None:
                  pass
              """
          )
        );
    }
}
