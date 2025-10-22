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
package org.openrewrite.java.format;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class SourcePositionServiceTest implements RewriteTest {

    @DocumentExample
    @Test
    void correctlyCalculatesLineLength() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(20);
                  return super.visitPackage(pkg, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(461);
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(72);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(365);
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(80);
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("valueOf".equals(method.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(80);
                  }
                  return super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J.Assert visitAssert(J.Assert _assert, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(28);
                  return super.visitAssert(_assert, ctx);
              }

              @Override
              public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(51);
                  return super.visitReturn(_return, ctx);
              }
          })),
          java(
            """
            package com.example;
            
            // Comments should not affect line length calculation
            public class Test {
                public int /* multiline comments can impact though */ example() {
                    String text = "This is a sample string to test line length calculation";
                    assert text != null;
                    // Another comment that is not counted
                    String invocation = String.valueOf("Both lines share the same length.");
                    return text.length() + invocation.length();
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
