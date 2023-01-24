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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

class ExtractInterfaceTest implements RewriteTest {

    private static class ExtractTestInterface extends Recipe {
        @Override
        public String getDisplayName() {
            return "Extract interface";
        }

        @Override
        public String getDescription() {
            return "Extract to an interface named `ITest`.";
        }

        @Override
        protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
            return ListUtils.flatMap(before, b ->
              ExtractInterface.extract((JavaSourceFile) b,
                "org.openrewrite.interfaces.ITest"));
        }
    }

    @Test
    void extractInterface() {
        rewriteRun(
          spec -> spec.recipe(new ExtractTestInterface()).cycles(1).expectedCyclesThatMakeChanges(1),
          java(
            """
              package org.openrewrite;

              class Test {
                  int f;

                  public Test() {
                  }

                  public final int test() {
                      return 0;
                  }

                  private int privateTest() {
                  }

                  public static int staticTest() {
                  }
              }
              """,
            """
              package org.openrewrite;
                            
              import org.openrewrite.interfaces.ITest;
                            
              class Test implements ITest {
                  int f;
                  
                  public Test() {
                  }
                            
                  @Override
                  public final int test() {
                      return 0;
                  }
                  
                  private int privateTest() {
                  }
                  
                  public static int staticTest() {
                  }
              }
              """
          ),
          java(
            null,
            """
                  package org.openrewrite.interfaces;
                  
                  interface ITest {
                  
                      int test();
                  }
              """,
            spec -> spec.path("org/openrewrite/interfaces/ITest.java")
          )
        );
    }
}
