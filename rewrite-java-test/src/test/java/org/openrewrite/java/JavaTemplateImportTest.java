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
package org.openrewrite.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;


/**
 * These JavaTemplate tests are specific to the `import` matching syntax.
 */
class JavaTemplateImportTest implements RewriteTest {

    @Disabled
    @Test
    void replaceImport() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitImport(J.Import import_, ExecutionContext executionContext) {
                      return JavaTemplate.apply("import #{}", getCursor(),
                        import_.getCoordinates().replace(), "java.util.Stack");
                  }
              }
            )),
          java(
            """
              import java.util.List;
              class A {
              }
                """,
            """
              import java.util.Stack;
              class A {
              }
              """
          )
        );
    }
}
