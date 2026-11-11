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

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.java.Assertions.java;

class ExtractInterfaceTest implements RewriteTest {

    @AllArgsConstructor
    private static class ExtractTestInterface extends ScanningRecipe<AtomicReference<String>> {
        @Override
        public String getDisplayName() {
            return "Extract interface";
        }

        @Override
        public String getDescription() {
            return "Extract to an interface named `ITest`.";
        }

        @Override
        public AtomicReference<String> getInitialValue(ExecutionContext ctx) {
            return new AtomicReference<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicReference<String> acc) {
            return new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    J.ClassDeclaration c = cu.getClasses().isEmpty() ? null : cu.getClasses().getFirst();
                    // after the extraction the class implements the interface, so nothing is extracted again
                    if (c != null && c.getKind() == J.ClassDeclaration.Kind.Type.Class && c.getImplements() == null) {
                        acc.set(cu.printAll());
                    }
                    return cu;
                }
            };
        }

        @Override
        public Collection<? extends SourceFile> generate(AtomicReference<String> acc, ExecutionContext ctx) {
            if (acc.get() == null) {
                return List.of();
            }
            J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(acc.get())
              .findFirst().orElseThrow();
            return List.of((SourceFile) new ExtractInterface.CreateInterface("org.openrewrite.interfaces.ITest")
              .visitNonNull(cu, ctx));
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(AtomicReference<String> acc) {
            return new ExtractInterface.ImplementAndAddOverrideAnnotations("org.openrewrite.interfaces.ITest");
        }
    }

    @DocumentExample
    @Test
    void extractInterface() {
        rewriteRun(
          spec -> spec.recipe(new ExtractTestInterface()),
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
            doesNotExist(),
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
