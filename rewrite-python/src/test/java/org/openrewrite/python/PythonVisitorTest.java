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
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

/**
 * Verifies that Python type positions are routed through {@link PythonVisitor#visitTypeName}, so
 * recipes hooking type names (e.g. {@code ChangeType}, {@code FindTypes}) see them on Python trees
 * just as they do on Java trees.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class PythonVisitorTest implements RewriteTest {

    private static List<String> collectTypeNames(Py.CompilationUnit cu) {
        List<String> names = new ArrayList<>();
        new PythonVisitor<List<String>>() {
            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, List<String> ns) {
                ns.add(nameTree.getClass().getSimpleName() + ":" + nameTree.printTrimmed(getCursor()));
                return nameTree;
            }
        }.visit(cu, names);
        return names;
    }

    @Test
    void parameterAnnotationIsVisitedAsTypeName() {
        rewriteRun(
          python(
            """
              from typing import List

              def foo(items: List[str]) -> List[int]:
                  pass
              """,
            spec -> spec.afterRecipe(cu -> assertThat(collectTypeNames(cu))
              .contains("ParameterizedType:List[str]"))
          )
        );
    }

    @Test
    void returnTypeHintTreeIsVisitedAsTypeName() {
        rewriteRun(
          python(
            """
              class Box:
                  pass

              def g() -> Box:
                  pass
              """,
            spec -> spec.afterRecipe(cu -> assertThat(collectTypeNames(cu))
              .contains("Identifier:Box"))
          )
        );
    }

    @Test
    void exceptionTypeExpressionIsVisitedAsTypeName() {
        rewriteRun(
          python(
            """
              try:
                  pass
              except ValueError:
                  pass
              """,
            spec -> spec.afterRecipe(cu -> assertThat(collectTypeNames(cu))
              .contains("Identifier:ValueError"))
          )
        );
    }

    @Test
    void unionTypeMembersAreVisitedAsTypeNames() {
        rewriteRun(
          python(
            """
              def f(x: int | str) -> None:
                  pass
              """,
            spec -> spec.afterRecipe(cu -> assertThat(collectTypeNames(cu))
              .contains("Identifier:int", "Identifier:str"))
          )
        );
    }

    @Test
    void typeAliasValueIsVisitedAsTypeName() {
        rewriteRun(
          python(
            """
              from typing import List

              type Aliased = List
              """,
            spec -> spec.afterRecipe(cu -> assertThat(collectTypeNames(cu))
              .contains("Identifier:List"))
          )
        );
    }
}
