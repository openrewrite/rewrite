/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions", "ClassInitializerMayBeStatic"})
class UnwrapParenthesesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                doAfterVisit(new UnwrapParentheses<>(parens));
                return super.visitParentheses(parens, ctx);
            }
        }));
    }

    @Test
    void unwrapAssignment() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean a;
                  {
                      a = (true);
                  }
              }
              """,
            """
              public class A {
                  boolean a;
                  {
                      a = true;
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapIfCondition() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      if((true)) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      if(true) {}
                  }
              }
              """
          )
        );
    }
}
