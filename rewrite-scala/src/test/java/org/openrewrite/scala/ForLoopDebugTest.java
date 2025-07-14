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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ForLoopDebugTest implements RewriteTest {

    @Test
    void debugRangeBasedForLoop() {
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    System.out.println("=== AST Debug Output ===");
                    System.out.println(cu.printTrimmed());
                    System.out.println("=== End AST Debug Output ===");
                    return super.visitCompilationUnit(cu, ctx);
                }
                
                @Override
                public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                    System.out.println("Found J.ForLoop!");
                    System.out.println("Control: " + forLoop.getControl());
                    System.out.println("Init: " + forLoop.getControl().getInit());
                    System.out.println("Condition: " + forLoop.getControl().getCondition());
                    System.out.println("Update: " + forLoop.getControl().getUpdate());
                    System.out.println("Body: " + forLoop.getBody());
                    System.out.println("Markers: " + forLoop.getMarkers());
                    forLoop.getMarkers().findFirst(org.openrewrite.scala.marker.ScalaForLoop.class)
                        .ifPresent(marker -> System.out.println("ScalaForLoop marker found with source: " + marker.getOriginalSource()));
                    return super.visitForLoop(forLoop, ctx);
                }
                
                @Override
                public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, ExecutionContext ctx) {
                    System.out.println("Found J.ForEachLoop!");
                    System.out.println("Control: " + forEachLoop.getControl());
                    System.out.println("Variable: " + forEachLoop.getControl().getVariable());
                    System.out.println("Iterable: " + forEachLoop.getControl().getIterable());
                    System.out.println("Body: " + forEachLoop.getBody());
                    return super.visitForEachLoop(forEachLoop, ctx);
                }
                
                @Override
                public J.Unknown visitUnknown(J.Unknown unknown, ExecutionContext ctx) {
                    System.out.println("Found J.Unknown: " + unknown.getSource());
                    return super.visitUnknown(unknown, ctx);
                }
            })),
            scala(
                """
                object Test {
                  for (i <- 0 until 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void debugCollectionForLoop() {
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, ExecutionContext ctx) {
                    System.out.println("Found J.ForEachLoop for collection!");
                    return super.visitForEachLoop(forEachLoop, ctx);
                }
            })),
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  for (item <- list) {
                    println(item)
                  }
                }
                """
            )
        );
    }

    @Test
    void debugCompoundAssignment() {
        // Let's print raw Scala code to check what actual node type it is
        System.out.println("Scala code for compound assignment:");
        System.out.println("x += 5");
        System.out.println("This is typically desugared to: x = x + 5");
        
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.Unknown visitUnknown(J.Unknown unknown, ExecutionContext ctx) {
                    if (unknown.getSource().getText().contains("+=")) {
                        System.out.println("Found J.Unknown for compound assignment: " + unknown.getSource());
                        System.out.println("This should be mapped to J.AssignmentOperation");
                    }
                    return super.visitUnknown(unknown, ctx);
                }
                
                @Override
                public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
                    System.out.println("Found J.AssignmentOperation!");
                    System.out.println("Variable: " + assignOp.getVariable());
                    System.out.println("Operator: " + assignOp.getOperator());
                    System.out.println("Assignment: " + assignOp.getAssignment());
                    return super.visitAssignmentOperation(assignOp, ctx);
                }
            })),
            scala(
                """
                object Test {
                  var x = 10
                  x += 5
                }
                """
            )
        );
    }
    
    @Test
    void debugAssignment() {
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    System.out.println("=== Assignment AST Debug Output ===");
                    System.out.println(cu.printTrimmed());
                    System.out.println("=== End Assignment AST Debug Output ===");
                    
                    // Let's see the full structure
                    cu.getClasses().forEach(clazz -> {
                        System.out.println("Class: " + clazz.getSimpleName());
                        if (clazz.getBody() != null) {
                            clazz.getBody().getStatements().forEach(stmt -> {
                                System.out.println("  Statement type: " + stmt.getClass().getSimpleName());
                                System.out.println("  Statement: " + stmt);
                            });
                        }
                    });
                    
                    return super.visitCompilationUnit(cu, ctx);
                }
                
                @Override
                public J.Unknown visitUnknown(J.Unknown unknown, ExecutionContext ctx) {
                    System.out.println("Found J.Unknown: " + unknown.getSource());
                    return super.visitUnknown(unknown, ctx);
                }
                
                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                    System.out.println("Found J.Assignment!");
                    System.out.println("Variable: " + assignment.getVariable());
                    System.out.println("Assignment: " + assignment.getAssignment());
                    return super.visitAssignment(assignment, ctx);
                }
                
                @Override
                public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
                    System.out.println("Found J.AssignmentOperation!");
                    System.out.println("Variable: " + assignOp.getVariable());
                    System.out.println("Operator: " + assignOp.getOperator());
                    System.out.println("Assignment: " + assignOp.getAssignment());
                    return super.visitAssignmentOperation(assignOp, ctx);
                }
            })),
            scala(
                """
                object Test {
                  var x = 0
                  x = 5
                  x += 10
                }
                """
            )
        );
    }
}