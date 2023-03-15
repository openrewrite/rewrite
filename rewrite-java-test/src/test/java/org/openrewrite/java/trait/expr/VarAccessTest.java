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
package org.openrewrite.java.trait.expr;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
public class VarAccessTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J preVisit(J tree, ExecutionContext executionContext) {
                return VarAccess.viewOf(getCursor())
                  .map(var -> {
                      assertNotNull(var.getName(), "VarAccess.getName() is null");
                      assertNotNull(var.getVariable(), "VarAccess.getVariable() is null");
                      return SearchResult.foundMerging(tree, var.getName() + " local:" + var.isLocal() + " l:" + var.isLValue() + " r:" + var.isRValue() + " q:" + var.hasQualifier());
                  })
                  .orSuccess(tree);
            }
        })).cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void correctlyLabelsLocalVariables() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(int a) {
                      int i = a;
                      i = 1;
                  }
              }
              """,
            """
              class Test {
                  void test(int a) {
                      int i = /*~~(a local:true l:false r:true q:false)~~>*/a;
                      /*~~(i local:true l:true r:false q:false)~~>*/i = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void correctlyLabelsLocalVariablesParenthesesWrapped() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(int a) {
                      int i = (a);
                      (i) = 1;
                  }
              }
              """,
            """
              class Test {
                  void test(int a) {
                      int i = (/*~~(a local:true l:false r:true q:false)~~>*/a);
                      (/*~~(i local:true l:true r:false q:false)~~>*/i) = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void correctlyLabelsLocalVariablesDoubleParenthesesWrapped() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(int a) {
                      int i = ((a));
                      ((i)) = 1;
                  }
              }
              """,
            """
              class Test {
                  void test(int a) {
                      int i = ((/*~~(a local:true l:false r:true q:false)~~>*/a));
                      ((/*~~(i local:true l:true r:false q:false)~~>*/i)) = 1;
                  }
              }
              """
          )
        );
    }
}
