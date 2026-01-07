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
package org.openrewrite.java;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.CompilationUnit;

/**
 * This test reproduces a StackOverflowError when JavaTypeVisitor encounters
 * circular type references in parameterized types.
 *
 * Common in builder patterns like: {@code interface Builder<T extends Builder<T>>}
 * and the OpenSearch Java client.
 */
class JavaTypeVisitorStackOverflowTest {

    /**
     * Integration test with real Java code that has recursive generic bounds.
     *
     * This parses actual Java code with builder patterns and attempts to visit all types.
     * Without fix: StackOverflowError
     * With fix: Should complete normally
     */
    @Test
    void testRealWorldBuilderPatternCausesStackOverflow() {
        String javaSource = """
            package com.example;

            public class RecursiveBuilder {
                // This is the pattern that causes StackOverflowError
                interface Builder<T extends Builder<T>> {
                    T self();
                }

                static class ConcreteBuilder implements Builder<ConcreteBuilder> {
                    @Override
                    public ConcreteBuilder self() {
                        return this;
                    }
                }

                public void useBuilder(Builder<?> builder) {
                    builder.self();
                }
            }
            """;

        assertThatCode(() -> {
            // Parse the code
            ExecutionContext ctx = new org.openrewrite.InMemoryExecutionContext();
            CompilationUnit cu = JavaParser.fromJavaVersion()
                    .build()
                    .parse(ctx, javaSource)
                    .findFirst()
                    .map(CompilationUnit.class::cast)
                    .orElseThrow();

            // Visit all types in the AST - this triggers the StackOverflowError
            JavaTypeVisitor<Integer> typeVisitor = new JavaTypeVisitor<>();

            new JavaIsoVisitor<Integer>() {
                @Override
                public ClassDeclaration visitClassDeclaration(ClassDeclaration classDecl, Integer p) {
                    if (classDecl.getType() != null) {
                        // WITHOUT FIX: StackOverflowError occurs here
                        typeVisitor.visit(classDecl.getType(), p);
                    }
                    return super.visitClassDeclaration(classDecl, p);
                }
            }.visit(cu, 0);

            // If we reach here, the fix is working
        }).doesNotThrowAnyException();
    }
}
