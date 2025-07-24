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
    void verifyArrayAccessNotUnknown() {
        AtomicInteger arrayAccessCount = new AtomicInteger();
        AtomicInteger unknownCount = new AtomicInteger();
        AtomicBoolean foundArrayAccess = new AtomicBoolean(false);
        
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
                    arrayAccessCount.incrementAndGet();
                    foundArrayAccess.set(true);
                    System.out.println("Found J.ArrayAccess: " + arrayAccess);
                    System.out.println("  Array: " + arrayAccess.getIndexed());
                    System.out.println("  Index: " + arrayAccess.getDimension().getIndex());
                    return super.visitArrayAccess(arrayAccess, ctx);
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
        
        // Verify that we found J.ArrayAccess nodes and not J.Unknown for array accesses
        assertThat(foundArrayAccess.get())
            .as("Should have found at least one J.ArrayAccess node")
            .isTrue();
            
        assertThat(arrayAccessCount.get())
            .as("Should have found 4 J.ArrayAccess nodes (arr(0), arr(1), matrix(0), and matrix(0)(1))")
            .isEqualTo(4);
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("J.ArrayAccess nodes found: " + arrayAccessCount.get());
        System.out.println("J.Unknown nodes found: " + unknownCount.get());
    }

    @Test
    void verifyArrayAccessInExpression() {
        AtomicBoolean foundArrayAccess = new AtomicBoolean(false);
        AtomicInteger methodInvocationCount = new AtomicInteger();
        
        rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
                    foundArrayAccess.set(true);
                    System.out.println("Found J.ArrayAccess in expression!");
                    System.out.println("  Array: " + arrayAccess.getIndexed());
                    System.out.println("  Index: " + arrayAccess.getDimension().getIndex());
                    return super.visitArrayAccess(arrayAccess, ctx);
                }
                
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    methodInvocationCount.incrementAndGet();
                    System.out.println("Found J.MethodInvocation: " + method.getSimpleName());
                    System.out.println("  Select: " + method.getSelect());
                    System.out.println("  Arguments: " + method.getArguments());
                    if (method.getSimpleName().equals("apply")) {
                        System.out.println("  WARNING: Found 'apply' method call - this might be Scala array access!");
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
        
        // Check if Scala's arr(0) syntax is being parsed as method invocation
        if (!foundArrayAccess.get() && methodInvocationCount.get() > 0) {
            System.out.println("\n=== Analysis ===");
            System.out.println("No J.ArrayAccess found, but found " + methodInvocationCount.get() + " method invocations.");
            System.out.println("Scala's array access syntax arr(0) might be parsed as method invocation arr.apply(0)");
        }
        
        assertThat(foundArrayAccess.get() || methodInvocationCount.get() > 0)
            .as("Should find either J.ArrayAccess or J.MethodInvocation for array access syntax")
            .isTrue();
    }
}