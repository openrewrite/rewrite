package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class LengthCalculatorTest implements RewriteTest {

    @DocumentExample
    @Test
    void correctlyCalculatesLineLength() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Override
              public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
                  assertThat(LengthCalculator.computeTreeLineLength(pkg, getCursor())).isEqualTo(20);
                  return super.visitPackage(pkg, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertThat(LengthCalculator.computeTreeLineLength(classDecl, getCursor())).isEqualTo(381);
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertThat(LengthCalculator.computeTreeLineLength(classDecl, getCursor())).isEqualTo(72);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  assertThat(LengthCalculator.computeTreeLineLength(method, getCursor())).isEqualTo(285);
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  assertThat(LengthCalculator.computeTreeLineLength(multiVariable, getCursor())).isEqualTo(80);
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  assertThat(LengthCalculator.computeTreeLineLength(method, getCursor())).isEqualTo(80);
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
            package com.example;
            
            // Comments should not affect line length calculation
            public class Test {
                public void /* multiline comments can impact though */ example() {
                    String text = "This is a sample string to test line length calculation";
                    // Another comment that is not counted
                    String invocation = String.valueOf("Both lines share the same length.");
                }
            
                class Inner {
                    // Inner class to test nested structures
                }
            }
            """
          )
        );
    }
}