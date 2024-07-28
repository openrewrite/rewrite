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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds matching method invocations.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class FindMethods extends Recipe {
    transient MethodCalls methodCalls = new MethodCalls(this);

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Find method usages";
    }

    @Override
    public String getDescription() {
        return "Find method calls by pattern.";
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern, matchOverrides), new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                // In an annotation @Example(value = "") the identifier "value" may have a method type
                J.Identifier i = super.visitIdentifier(identifier, ctx);
                if(i.getType() instanceof JavaType.Method && methodMatcher.matches((JavaType.Method) i.getType())
                   && !(getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation)) {
                    JavaType.Method m = (JavaType.Method) i.getType();
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if(javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(
                                javaSourceFile.getSourcePath().toString(),
                                m.getName(),
                                m.getDeclaringType().getFullyQualifiedName(),
                                m.getName(),
                                m.getParameterTypes().stream()
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(", "))

                        ));
                    }
                    i = SearchResult.found(i);
                }
                return i;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(
                                javaSourceFile.getSourcePath().toString(),
                                method.printTrimmed(getCursor()),
                                method.getMethodType().getDeclaringType().getFullyQualifiedName(),
                                method.getSimpleName(),
                                method.getArguments().stream()
                                        .map(Expression::getType)
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(", "))
                        ));
                    }
                    m = SearchResult.found(m);
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getMethodType())) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(
                                javaSourceFile.getSourcePath().toString(),
                                memberRef.printTrimmed(getCursor()),
                                memberRef.getMethodType().getDeclaringType().getFullyQualifiedName(),
                                memberRef.getMethodType().getName(),
                                memberRef.getArguments().stream()
                                        .map(Expression::getType)
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(", "))
                        ));
                    }
                    m = m.withReference(SearchResult.found(m.getReference()));
                }
                return m;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (methodMatcher.matches(newClass)) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(
                                javaSourceFile.getSourcePath().toString(),
                                newClass.printTrimmed(getCursor()),
                                newClass.getType().toString(),
                                "<constructor>",
                                newClass.getArguments().stream()
                                        .map(Expression::getType)
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(", "))
                        ));
                    }
                    n = SearchResult.found(n);
                }
                return n;
            }
        });
    }

    public static Set<J> find(J j, String methodPattern) {
        return find(j, methodPattern, false);
    }

    /**
     * @param j              The subtree to search.
     * @param methodPattern  A method pattern. See {@link MethodMatcher} for details about this syntax.
     * @param matchOverrides Whether to match overrides.
     * @return A set of {@link J.MethodInvocation}, {@link J.MemberReference}, and {@link J.NewClass} representing calls to this method.
     */
    public static Set<J> find(J j, String methodPattern, boolean matchOverrides) {
        FindMethods findMethods = new FindMethods(methodPattern, matchOverrides);
        findMethods.methodCalls.setEnabled(false);
        return TreeVisitor.collect(
                        findMethods.getVisitor(),
                        j,
                        new HashSet<>()
                )
                .stream()
                .filter(t -> t instanceof J.MethodInvocation || t instanceof J.MemberReference || t instanceof J.NewClass)
                .map(t -> (J) t)
                .collect(Collectors.toSet());
    }

    public static Set<J.MethodDeclaration> findDeclaration(J j, String methodPattern) {
        return findDeclaration(j, methodPattern, false);
    }

    public static Set<J.MethodDeclaration> findDeclaration(J j, String methodPattern, boolean matchOverrides) {
        return TreeVisitor.collect(
                        new JavaIsoVisitor<ExecutionContext>() {
                            final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

                            @Override
                            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                                if (enclosingClass != null && methodMatcher.matches(method, getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class))) {
                                    return SearchResult.found(method);
                                } else if (methodMatcher.matches(method.getMethodType())) {
                                    return SearchResult.found(method);
                                }
                                return super.visitMethodDeclaration(method, ctx);
                            }
                        },
                        j,
                        new HashSet<>()
                )
                .stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .collect(Collectors.toSet());
    }
}
