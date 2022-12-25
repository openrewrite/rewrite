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
package org.openrewrite.java.dataflow;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("FunctionName")
class FindLocalTaintFlowToExternalSinkTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new FindLocalFlowPaths<>(new LocalTaintFlowSpec<J.MethodInvocation, Expression>() {
            @Override
            public boolean isSource(J.MethodInvocation methodInvocation, Cursor cursor) {
                return methodInvocation.getSimpleName().equals("source");
            }

            @Override
            public boolean isSink(Expression sink, Cursor cursor) {
                return ExternalSinkModels.getInstance().isSinkNode(sink, cursor, "create-file");
            }
        }))).expectedCyclesThatMakeChanges(1).cycles(1);
    }

    @Test
    void taintFromStringToCreateFile() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.FileOutputStream;
              class Test {
                  File source() { return null; }
                  void test(String contents) {
                      File f = source();
                      try (FileOutputStream fos = new FileOutputStream(f)) {
                          fos.write(contents.getBytes());
                      }
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.FileOutputStream;
              class Test {
                  File source() { return null; }
                  void test(String contents) {
                      File f = /*~~>*/source();
                      try (FileOutputStream fos = new FileOutputStream(/*~~>*/f)) {
                          fos.write(contents.getBytes());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFromStringThroughFileToCreateFile() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.FileOutputStream;
              class Test {
                  String source() { return null; }
                  void test(String contents) {
                      String s = source();
                      File f = new File(s);
                      try (FileOutputStream fos = new FileOutputStream(f)) {
                          fos.write(contents.getBytes());
                      }
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.FileOutputStream;
              class Test {
                  String source() { return null; }
                  void test(String contents) {
                      String s = /*~~>*/source();
                      File f = /*~~>*/new File(/*~~>*/s);
                      try (FileOutputStream fos = new FileOutputStream(/*~~>*/f)) {
                          fos.write(contents.getBytes());
                      }
                  }
              }
              """
          )
        );
    }
}
