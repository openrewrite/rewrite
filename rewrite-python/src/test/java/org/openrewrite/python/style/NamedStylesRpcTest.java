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
package org.openrewrite.python.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.python.tree.Py;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.python.Assertions.python;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NamedStylesRpcTest implements RewriteTest {

    @Test
    void stylesOnTheSourceFileReachThePythonFormatter() {
        SpacesStyle spaces = IntelliJ.spaces();
        NamedStyles spaceBeforeComma = new NamedStyles(randomId(), "test", "test", null, emptySet(),
          singletonList(spaces.withOther(spaces.getOther().withBeforeComma(true))));

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PythonIsoVisitor<>() {
              @Override
              public Py.CompilationUnit visitCompilationUnit(Py.CompilationUnit cu, ExecutionContext ctx) {
                  return autoFormat(cu, ctx);
              }
          })),
          python(
            """
              def f(a,b):
                  pass
              """,
            """
              def f(a , b):
                  pass
              """,
            spec -> spec.mapBeforeRecipe(cu -> cu.withMarkers(cu.getMarkers().add(spaceBeforeComma)))
          )
        );
    }
}
