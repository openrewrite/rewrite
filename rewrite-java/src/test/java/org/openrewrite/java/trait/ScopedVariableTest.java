/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ScopedVariableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() ->
          new ScopedVariable.Matcher().asVisitor(a -> SearchResult.found(a.getTree(),
              String.format("Identified variable %s within scope %s", a.getIdentifier(), a.getScope().getValue().getClass().getSimpleName())
            )
          )
        ));
    }

    @Test
    @DocumentExample
    void markAllFieldsInClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  private String field;
              
                  void doSome() {
                      String anotherField;
                  }
              }
              """,
            """
              class Test {
                  private String /*~~(Identified variable field within scope Block)~~>*/field;
              
                  void doSome() {
                      String /*~~(Identified variable anotherField within scope Block)~~>*/anotherField;
                  }
              }
              """
          )
        );
    }

    @Test
    void markAllFieldsInMethod() {
        rewriteRun(
          spec -> spec.recipe(Recipe.noop()),
          //language=java
          java(
            """
              class Test {
                  private String field;
              
                  void doSome() {
                      String anotherField;
                  }
              }
              """,
            spec -> spec.beforeRecipe((source) -> {
                J.MethodDeclaration md = (J.MethodDeclaration) source.getClasses().get(0).getBody().getStatements().get(1);
                new ScopedVariable.Matcher().asVisitor(var -> {
                    assertThat(((J.Block) var.getScope().getValue())).isEqualTo(md.getBody());
                    return var.getTree();
                }).visit(md, new InMemoryExecutionContext());
            })
          )
        );
    }

}
