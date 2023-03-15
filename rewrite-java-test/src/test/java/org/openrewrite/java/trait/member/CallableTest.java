/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.member;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class CallableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J preVisit(J tree, ExecutionContext executionContext) {
                return StaticInitializerMethod
                  .viewOf(getCursor())
                  .map(c -> SearchResult.foundMerging(tree, c.getName()))
                  .bind(t -> InstanceInitializer.viewOf(getCursor())
                    .map(c -> SearchResult.foundMerging(t, c.getName())))
                  .orSuccess(t ->
                    Method.viewOf(getCursor())
                      .map(c -> SearchResult.foundMerging(tree, c.getName()))
                      .orSuccess(tree));
            }
        })).cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void searchResultsForCallables() {
        rewriteRun(
          java("""
            class Test {
                Test() {}
                void test() {}
            }
            """,
            """
            class Test /*~~(<clinit>, <obinit>)~~>*/{
                /*~~(Test)~~>*/Test() {}
                /*~~(test)~~>*/void test() {}
            }
            """
          )
        );
    }
}
