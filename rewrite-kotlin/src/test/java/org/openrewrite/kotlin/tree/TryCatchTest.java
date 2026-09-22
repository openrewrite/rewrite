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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class TryCatchTest implements RewriteTest {

    @SuppressWarnings("CatchMayIgnoreException")
    @Test
    void tryCatchNoFinally() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  try {
                  }  catch   (    ex :  Exception  /*c*/   )     {
                  }
              }
              """
          )
        );
    }

    @Test
    void tryFinally() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  try {
                  }  finally   {
                      val x = 0
                  }
              }
              """
          )
        );
    }

    @Test
    void tryAsAVariable() {
        rewriteRun(
          kotlin(
            """
              val throwable : Throwable? = try {
              } catch ( caught : Throwable ) {
                 caught
              } as? Throwable
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/286")
    @Test
    void catchUnderscore() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  try {
                  } catch (_: InterruptedException) {
                  }
              }
              """
          )
        );
    }

    @Test
    void markersOnCatchParameterPrintedWithTrailingComma() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<ExecutionContext>() {
              @Override
              public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, ExecutionContext ctx) {
                  JRightPadded<T> tree = controlParens.getPadding().getTree();
                  return controlParens.getPadding().withTree(
                    tree.withMarkers(tree.getMarkers().addIfAbsent(new SearchResult(Tree.randomId(), null))));
              }
          })),
          kotlin(
            """
              fun f() {
                  try {
                      println()
                  } catch (e: Exception,) {
                  }
              }
              """,
            """
              fun f() {
                  try {
                      println()
                  } catch (/*~~>*/e: Exception,) {
                  }
              }
              """
          )
        );
    }
}
