/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class GroovyVisitorTest implements RewriteTest {

    @DocumentExample
    @Test
    void autoFormatIncludesOmitParentheses() {
        rewriteRun(
          spec -> spec
            .recipeExecutionContext(new InMemoryExecutionContext().addObserver(new TreeObserver.Subscription(new TreeObserver() {
                @Override
                public Tree treeChanged(Cursor cursor, Tree newTree) {
                    return newTree;
                }
            }).subscribeToType(J.Lambda.class)))
            .recipe(RewriteTest.toRecipe(() -> new GroovyVisitor<>() {
                @Override
                public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    return autoFormat(super.visitCompilationUnit(cu, ctx), ctx);
                }
            })),
          groovy(
            """
              Test.test({ it })
              """,
            """
              Test.test {
                  it
              }
              """
          )
        );
    }
}
