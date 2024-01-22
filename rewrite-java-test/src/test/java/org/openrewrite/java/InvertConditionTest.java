/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class InvertConditionTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions", "InfiniteRecursion"})
    @Test
    void invertCondition() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.If visitIf(J.If iff, ExecutionContext ctx) {
                    return iff.withIfCondition(InvertCondition.invert(iff.getIfCondition(), getCursor()));
                }
            }).withMaxCycles(1)),
          java(
            """
              class Test {
                  boolean a;
                  boolean b;
                            
                  boolean test() {
                      if(!b) {}
                      if(b || a) {}
                      if(1 < 2) {}
                      if(b) {}
                      if((b)) {}
                      if(test()) {}
                      if(this.test()) {}
                      return true;
                  }
              }
              """,
            """
              class Test {
                  boolean a;
                  boolean b;
                            
                  boolean test() {
                      if(b) {}
                      if(!(b || a)) {}
                      if(1 >= 2) {}
                      if(!b) {}
                      if(!(b)) {}
                      if(!test()) {}
                      if(!this.test()) {}
                      return true;
                  }
              }
              """
          )
        );
    }
}
