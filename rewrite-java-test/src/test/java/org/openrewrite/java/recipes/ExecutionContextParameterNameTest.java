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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ExecutionContextParameterNameTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ExecutionContextParameterName())
          .parser(JavaParser.fromJavaVersion()
            .dependsOn(

              """
                package org.openrewrite;
                public class Recipe {}
                """,
              """
                package org.openrewrite;
                public class Visitor {}
                """,
              """
                package org.openrewrite;
                public class TreeVisitor extends Visitor {}
                """,
              """
                package org.openrewrite;
                public class ExecutionContext {}
                """
            ));
    }

    @DocumentExample
    @Test
    void recipe() {
        rewriteRun(
          java(
            """
              import org.openrewrite.*;
              class SampleRecipe extends Recipe {
                  public void test(ExecutionContext executionContext) {}
              }
              """,
            """
              import org.openrewrite.*;
              class SampleRecipe extends Recipe {
                  public void test(ExecutionContext ctx) {}
              }
              """
          )
        );
    }


    @Test
    void visitor() {
        rewriteRun(
          java(
            """
              import org.openrewrite.*;
              class SampleVisitor extends Visitor {
                  public void test(ExecutionContext executionContext) {}
              }
              """,
            """
              import org.openrewrite.*;
              class SampleVisitor extends Visitor {
                  public void test(ExecutionContext ctx) {}
              }
              """
          )
        );
    }

    @Test
    void treeVisitor() {
        rewriteRun(
          java(
            """
              import org.openrewrite.*;
              class SampleTreeVisitor extends TreeVisitor {
                  public void test(ExecutionContext executionContext) {}
              }
              """,
            """
              import org.openrewrite.*;
              class SampleTreeVisitor extends TreeVisitor {
                  public void test(ExecutionContext ctx) {}
              }
              """
          )
        );
    }
}
