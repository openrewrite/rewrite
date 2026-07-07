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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Exercises {@link UsesImport} the way it is actually used: as a precondition.
 * The gated editor bumps the integer literal {@code 1} to {@code 2}, so a match
 * applies the edit and a non-match leaves the file untouched -- which is exactly
 * the "skip the visit when the module isn't imported" behavior the precondition
 * provides on the recipe-execution host. (A normal text edit is used rather than
 * a {@code SearchResult} marker because marker rendering on a Python LST round-
 * trips to empty through the RPC printer, which would obscure the assertion.)
 */
class UsesImportTest implements RewriteTest {

    /** Gate a "bump integer literal 1 -> 2" editor on {@code uses_import(module)}. */
    private static Recipe gatedBumpLiteral(String module) {
        return toRecipe(() -> Preconditions.check(
          new UsesImport(module).getVisitor(),
          new PythonIsoVisitor<ExecutionContext>() {
              @Override
              public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if ("1".equals(literal.getValueSource())) {
                      return literal.withValue(2).withValueSource("2");
                  }
                  return literal;
              }
          }));
    }

    @Test
    void plainImportMatches() {
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("datetime")),
          python(
            """
              import datetime
              x = 1
              """,
            """
              import datetime
              x = 2
              """
          )
        );
    }

    @Test
    void fromImportMatchesModule() {
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("datetime")),
          python(
            """
              from datetime import datetime
              x = 1
              """,
            """
              from datetime import datetime
              x = 2
              """
          )
        );
    }

    @Test
    void submoduleMatchesParentQuery() {
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("os")),
          python(
            """
              import os.path
              x = 1
              """,
            """
              import os.path
              x = 2
              """
          )
        );
    }

    @Test
    void canonicalizationSafeTypingList() {
        // ty-types canonicalizes List -> list, so HasType("typing.List") would miss this
        // file. uses_import reads the import syntax and gates it in regardless.
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("typing")),
          python(
            """
              from typing import List
              x: List = []
              y = 1
              """,
            """
              from typing import List
              x: List = []
              y = 2
              """
          )
        );
    }

    @Test
    void noMatchWhenModuleNotImported() {
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("datetime")),
          python(
            """
              import os
              x = 1
              """
          )
        );
    }

    @Test
    void noMatchOnPartialModuleName() {
        // `os` must not match `ossaudiodev`.
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("os")),
          python(
            """
              import ossaudiodev
              x = 1
              """
          )
        );
    }

    @Test
    void fromImportedNameIsNotTreatedAsModule() {
        // `from os import path` imports the name `path`; querying module `path` must not match.
        rewriteRun(
          spec -> spec.recipe(gatedBumpLiteral("path")),
          python(
            """
              from os import path
              x = 1
              """
          )
        );
    }
}
