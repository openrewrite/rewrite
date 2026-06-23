/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinVisitorReturnTest implements RewriteTest {

    /**
     * A {@link org.openrewrite.kotlin.tree.K.Return} wraps a {@link J.Return}. A Java recipe (such as
     * {@code TestMethodsShouldBeVoid}) may rewrite that inner {@code J.Return} into a different statement,
     * e.g. unwrapping {@code return foo()} to {@code foo()}. {@code KotlinVisitor.visitReturn} previously
     * cast the visited expression straight back to {@code J.Return}, throwing a {@link ClassCastException}.
     */
    @Test
    void unwrapReturnIntoItsExpression() {
        rewriteRun(
          spec -> spec.recipe(new UnwrapReturn()),
          kotlin(
            """
              fun test() {
                  return bar()
              }

              fun bar() {
              }
              """,
            """
              fun test() {
                  bar()
              }

              fun bar() {
              }
              """
          )
        );
    }

    static class UnwrapReturn extends Recipe {
        @Override
        public String getDisplayName() {
            return "Unwrap a return into its expression";
        }

        @Override
        public String getDescription() {
            return "Replaces `return foo()` with `foo()`, mimicking how Java cleanup recipes rewrite the inner return.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaVisitor<ExecutionContext>() {
                @Override
                public J visitReturn(J.Return retrn, ExecutionContext ctx) {
                    Expression expression = retrn.getExpression();
                    if (expression != null) {
                        return expression.withPrefix(retrn.getPrefix());
                    }
                    return super.visitReturn(retrn, ctx);
                }
            };
        }
    }
}
