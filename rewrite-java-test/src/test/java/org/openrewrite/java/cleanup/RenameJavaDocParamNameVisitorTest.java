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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"JavadocDeclaration", "JavadocReference"})
class RenameJavaDocParamNameVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration md = method;
                if (md.getSimpleName().equals("method") && md.getParameters().stream()
                  .anyMatch(p -> p instanceof J.VariableDeclarations && ((J.VariableDeclarations) p).getVariables().stream()
                    .anyMatch(v -> v.getSimpleName().equals("oldName")))) {
                    md = new RenameJavaDocParamNameVisitor<>(md, "oldName", "newName")
                      .visitMethodDeclaration(md, executionContext);
                }
                return super.visitMethodDeclaration(md, executionContext);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, executionContext);
                if (variable.getSimpleName().equals("oldName")) {
                    v = v.withName(v.getName().withSimpleName("newName"));
                    v = v.withName(v.getName().withFieldType(v.getName().getFieldType().withName("newName")));
                    v = v.withVariableType(v.getVariableType().withName("newName"));
                }
                return v;
            }
        }));
    }

    @DocumentExample
    @Test
    void noJavaDocParamMatch() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * @param noMatch
                   */
                  void method(String oldName) {
                  }
              }
              """,
            """
              class Test {
                  /**
                   * @param noMatch
                   */
                  void method(String newName) {
                  }
              }
              """
          )
        );
    }

    @Test
    void renameParamName() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * @param oldName
                   */
                  void method(String oldName) {
                  }
              }
              """,
            """
              class Test {
                  /**
                   * @param newName
                   */
                  void method(String newName) {
                  }
              }
              """
          )
        );
    }
}
