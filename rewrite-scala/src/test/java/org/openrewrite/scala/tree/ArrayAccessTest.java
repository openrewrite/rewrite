/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

/**
 * Tests for Scala array access syntax.
 * 
 * Design Decision: In Scala, array access `arr(0)` is syntactic sugar for the apply() method.
 * We represent this as a J.MethodInvocation with a FunctionApplication marker rather than J.ArrayAccess.
 * This maintains semantic accuracy since Scala arrays are objects with apply() methods, not primitive arrays.
 */
class ArrayAccessTest implements RewriteTest {

    @Test
    void simpleArrayAccess() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  val first = arr(0)
                }
                """
            )
        );
    }

    @Test
    void nestedArrayAccess() {
        rewriteRun(
            scala(
                """
                object Test {
                  val matrix = Array(Array(1, 2), Array(3, 4))
                  val element = matrix(0)(1)
                }
                """
            )
        );
    }

    @Test
    void arrayAccessInExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(10, 20, 30)
                  val sum = arr(0) + arr(1)
                }
                """
            )
        );
    }

    @Test
    void listApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  val second = list(1)
                }
                """
            )
        );
    }

    @Test
    void mapApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val map = Map("a" -> 1, "b" -> 2)
                  val value = map("a")
                }
                """
            )
        );
    }

    @Test
    void arrayAccessWithVariable() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  val index = 2
                  val element = arr(index)
                }
                """
            )
        );
    }

    @Test
    void arrayAccessWithExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3, 4, 5)
                  val mid = arr(arr.length / 2)
                }
                """
            )
        );
    }

    @Test
    void stringApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val str = "hello"
                  val firstChar = str(0)
                }
                """
            )
        );
    }

    @Test
    void verifyArrayAccessAsFunctionApplication() {
        // In Scala, array access is syntactic sugar for apply() method calls.
        // We represent arr(0) as a J.MethodInvocation with FunctionApplication marker.
        AtomicInteger functionApplicationCount = new AtomicInteger();
        AtomicInteger unknownCount = new AtomicInteger();
        AtomicBoolean foundFunctionApplication = new AtomicBoolean(false);
        
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (method.getMarkers().findFirst(org.openrewrite.scala.marker.FunctionApplication.class).isPresent()) {
                        // Only count array access, not Array construction
                        if (method.getSelect() != null && 
                            !(method.getSelect() instanceof J.Identifier && 
                              ((J.Identifier) method.getSelect()).getSimpleName().equals("Array"))) {
                            functionApplicationCount.incrementAndGet();
                            foundFunctionApplication.set(true);
                            System.out.println("Found array access via function application: " + method);
                            System.out.println("  Target: " + method.getSelect());
                            System.out.println("  Arguments: " + method.getArguments());
                        }
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
                
                @Override
                public J.Unknown visitUnknown(J.Unknown unknown, ExecutionContext ctx) {
                    unknownCount.incrementAndGet();
                    System.out.println("Found J.Unknown: " + unknown.getSource());
                    // Check if this Unknown might be an array access that wasn't properly parsed
                    String source = unknown.getSource().getText();
                    if (source.contains("(") && source.contains(")") && !source.contains("val") && !source.contains("Array")) {
                        System.out.println("  WARNING: This Unknown might be an unparsed array access!");
                    }
                    return super.visitUnknown(unknown, ctx);
                }
                
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    System.out.println("=== AST Debug Output ===");
                    System.out.println(cu.printTrimmed());
                    System.out.println("=== End AST Debug Output ===");
                    return super.visitCompilationUnit(cu, ctx);
                }
            })),
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  val first = arr(0)
                  val second = arr(1)
                  
                  val matrix = Array(Array(1, 2), Array(3, 4))
                  val element = matrix(0)(1)
                }
                """
            )
        );
        
        // Verify that we found function application nodes and not J.Unknown for array accesses
        assertThat(foundFunctionApplication.get())
            .as("Should have found at least one function application (array access)")
            .isTrue();
            
        // Note: matrix(0)(1) counts as 2 separate function applications:
        // 1. matrix(0) returns an array
        // 2. The result is accessed with (1)
        assertThat(functionApplicationCount.get())
            .as("Should have found at least 2 function applications for array access")
            .isGreaterThanOrEqualTo(2);
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("Function application nodes found: " + functionApplicationCount.get());
        System.out.println("J.Unknown nodes found: " + unknownCount.get());
    }

    @Test
    void verifyArrayAccessInExpression() {
        AtomicBoolean foundFunctionApplication = new AtomicBoolean(false);
        AtomicInteger functionApplicationCount = new AtomicInteger();
        
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    System.out.println("Found J.MethodInvocation: " + method.getSimpleName());
                    System.out.println("  Select: " + method.getSelect());
                    System.out.println("  Arguments: " + method.getArguments());
                    if (method.getMarkers().findFirst(org.openrewrite.scala.marker.FunctionApplication.class).isPresent()) {
                        // Only count array access, not Array construction
                        if (method.getSelect() != null && 
                            !(method.getSelect() instanceof J.Identifier && 
                              ((J.Identifier) method.getSelect()).getSimpleName().equals("Array"))) {
                            foundFunctionApplication.set(true);
                            functionApplicationCount.incrementAndGet();
                            System.out.println("  -> This is array access via function application!");
                        }
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
                
                @Override
                public J.Unknown visitUnknown(J.Unknown unknown, ExecutionContext ctx) {
                    System.out.println("Found J.Unknown in expression: " + unknown.getSource());
                    return super.visitUnknown(unknown, ctx);
                }
            })),
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  // Just the array access in an expression context
                  println(arr(0))
                  println(arr(1) + arr(2))
                }
                """
            )
        );
        
        // Verify that array access is represented as function application
        assertThat(foundFunctionApplication.get())
            .as("Should have found function application markers for array access")
            .isTrue();
            
        assertThat(functionApplicationCount.get())
            .as("Should have found at least 3 function applications (arr(0), arr(1), arr(2))")
            .isGreaterThanOrEqualTo(3);
            
        System.out.println("\n=== Analysis ===");
        System.out.println("Function applications found: " + functionApplicationCount.get());
        System.out.println("Scala's array access syntax arr(0) is parsed as function application");
    }
}
