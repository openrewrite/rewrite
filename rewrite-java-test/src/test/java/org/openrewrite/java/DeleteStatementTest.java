/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("rawtypes")
class DeleteStatementTest implements RewriteTest {

    @Test
    void deleteField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  doAfterVisit(new DeleteStatement<>(multiVariable));
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              public class A {
                 List collection = null;
              }
              """,
            """
              public class A {
              }
              """
          )
        );
    }

    @SuppressWarnings("ALL")
    @Test
    void deleteSecondStatement() {
        //noinspection UnusedAssignment
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() != 4) {
                      return b;
                  }
                  List<Statement> statements = b.getStatements();
                  for (int i = 0; i < statements.size(); i++) {
                      Statement s = statements.get(i);
                      if (i == 1) {
                          doAfterVisit(new DeleteStatement<>(s));
                      }
                  }
                  return b;
              }
          })),
          java(
            """
              public class A {
                 {
                    String s = "";
                    s.toString();
                    s = "hello";
                    s.toString();
                 }
              }
              """,
            """
              public class A {
                 {
                    String s = "";
                    s = "hello";
                    s.toString();
                 }
              }
              """
          )
        );
    }

    @SuppressWarnings("ALL")
    @Test
    void deleteSecondAndFourthStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() != 4) {
                      return b;
                  }
                  List<Statement> statements = b.getStatements();
                  for (int i = 0; i < statements.size(); i++) {
                      Statement s = statements.get(i);
                      if (i == 1 || i == 3) {
                          doAfterVisit(new DeleteStatement<>(s));
                      }
                  }
                  return b;
              }
          })),
          java(
            """
              public class A {
                 {
                    String s = "";
                    s.toString();
                    s = "hello";
                    s.toString();
                 }
              }
              """,
            """
              public class A {
                 {
                    String s = "";
                    s = "hello";
                 }
              }
              """
          )
        );
    }

    @Test
    void deleteIfThenBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  J.If i = super.visitIf(iff, ctx);
                  if (!((J.Block) i.getThenPart()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(i.getThenPart()));
                  }
                  return i;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      if (true) {
                          int i = 0;
                      }
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      if (true){}
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteIfElseBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  J.If i = super.visitIf(iff, ctx);
                  if (!((J.Block) i.getElsePart().getBody()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(i.getElsePart().getBody()));
                  }
                  return i;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      if (false) {
                          int i = 0;
                      } else {
                          int i = 0;
                      }
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      if (false) {
                          int i = 0;
                      } else{}
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteForBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                  J.ForLoop f = super.visitForLoop(forLoop, ctx);
                  if (!((J.Block) f.getBody()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(f.getBody()));
                  }
                  return f;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      for (int i = 0; i < 1; i++) {
                          int j = 0;
                      }
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      for (int i = 0; i < 1; i++){}
                  }
              }
              """
          )
        );
    }

    @Test
    void dontDeleteForControl() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                  J.ForLoop f = super.visitForLoop(forLoop, ctx);
                  doAfterVisit(new DeleteStatement<>(f.getControl().getInit().get(0)));
                  doAfterVisit(new DeleteStatement<>(f.getControl().getUpdate().get(0)));
                  return f;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      for (int i = 0; i < 1; i++) {
                          int j = 0;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteForEachBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
                  J.ForEachLoop f = super.visitForEachLoop(forLoop, ctx);
                  doAfterVisit(new DeleteStatement<>(f.getControl().getVariable()));
                  return f;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      for (int i : new int[]{ 1 }) {
                          int j = 0;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dontDeleteForEachControl() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
                  J.ForEachLoop f = super.visitForEachLoop(forLoop, ctx);
                  if (!((J.Block) f.getBody()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(f.getBody()));
                  }
                  return f;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      for (int i : new int[]{ 1 }) {
                          int j = 0;
                      }
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      for (int i : new int[]{ 1 }){}
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteWhileBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext ctx) {
                  J.WhileLoop w = super.visitWhileLoop(whileLoop, ctx);
                  if (!((J.Block) w.getBody()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(w.getBody()));
                  }
                  return w;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      while (true) {
                          int i = 0;
                      }
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      while (true){}
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteDoWhileBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
                  J.DoWhileLoop w = super.visitDoWhileLoop(doWhileLoop, ctx);
                  if (!((J.Block) w.getBody()).getStatements().isEmpty()) {
                      doAfterVisit(new DeleteStatement<>(w.getBody()));
                  }
                  return w;
              }
          })),
          java(
            """
              public class A {
                  public void a() {
                      do {
                          int i = 0;
                      } while (true);
                  }
              }
              """,
            """
              public class A {
                  public void a() {
                      do{} while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteStatementInNegation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                  if (m.getSimpleName().equals("b")) {
                      doAfterVisit(new DeleteStatement<>(m));
                  }
                  return m;
              }
          })),
          java(
            """
              public abstract class A {
                  public void a() {
                      boolean a = !b();
                  }
                  
                  abstract boolean b();
              }
              """
          )
        );
    }
}
