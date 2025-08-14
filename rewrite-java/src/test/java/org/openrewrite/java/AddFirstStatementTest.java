/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.tree.J;
import org.openrewrite.test.AdHocRecipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AddFirstStatementTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AdHocRecipe("Test", "Test", false, () -> new JavaIsoVisitor<>(){
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                if(md.getBody() != null && md.getBody().getStatements().size() == 1) {
                    md = JavaTemplate.builder("String newLine = \"test\";")
                      .javaParser(JavaParser.fromJavaVersion())
                      .build()
                      .apply(getCursor(), md.getBody().getCoordinates().firstStatement());
                }
                return md;
            }
        }, null, null));
    }

    // Without the change this test succeeds
    @Test
    void firstStatementIsMethodInvocationWithAssignment() {
        //language=java
        rewriteRun(
          java(
            """
              public class Subject {
                  @Override
                  public void doSomething(Data o) {
                      int r = o.get();
                  }

                  private static interface Data  {
                      int get();
                  }
              }
              """,
            """
              public class Subject {
                  @Override
                  public void doSomething(Data o) {
                      String newLine = "test";
                      int r = o.get();
                  }
              
                  private static interface Data  {
                      int get();
                  }
              }
              """
          )
        );
    }

    // Without the change this test fails
    // The reason is that the new statement is an assignment:
    // String newLine = "test";
    // The javacoordinates are at md.getBody().getCoordinates().firstStatement():
    // o.get();
    // The template does not know what you want to do at this point. You might want to replace the method invocation with a constant "123" which would result in invalid code.
    // The template will automatically prefix the statement with "Object o = " to make sure the code compiles.
    // This is correct when replacing the statement, the end result would be:
    // Object o = "123";
    // In this case we are not replacing the statement, but prepending it. Because the block generator had no information about the mode, it would still apply the prefix:
    // Object o = String newLine = "test";
    // The fix to add the mode makes sure that when not replacing, the provided template should be valid in itself, so no modification is required.
    @Test
    void firstStatementIsMethodInvocationWithReturnTypeWithoutAssignment() {
        //language=java
        rewriteRun(
          java(
            """
              public class Subject {
                  @Override
                  public void doSomething(Data o) {
                      o.get();
                  }

                  private static interface Data  {
                      int get();
                  }
              }
              """,
            """
              public class Subject {
                  @Override
                  public void doSomething(Data o) {
                      String newLine = "test";
                      o.get();
                  }
              
                  private static interface Data  {
                      int get();
                  }
              }
              """
          )
        );
    }
}
