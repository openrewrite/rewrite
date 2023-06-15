/*
 * Copyright 2023 the original author or authors.
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTypeMethodMatcher;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

/**
 * This visitor is used to find method invocations that match a given method pattern as defined by {@link JavaTypeMethodMatcher}.
 * <p/>
 * This visitor is most often useful for testing custom implementations of {@link JavaTypeMethodMatcher}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FindMethodsVisitor extends JavaIsoVisitor<ExecutionContext> {
    private final JavaTypeMethodMatcher methodMatcher;
    @Nullable
    private final MethodCalls methodCalls;

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
        if (methodMatcher.matches(method)) {
            addToMethodCalls(method, ctx);
            m = SearchResult.found(m);
        }
        return m;
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
        J.MemberReference m = super.visitMemberReference(memberRef, ctx);
        if (methodMatcher.matches(m.getMethodType())) {
            addToMethodCalls(memberRef, ctx);
            m = m.withReference(SearchResult.found(m.getReference()));
        }
        return m;
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
        J.NewClass n = super.visitNewClass(newClass, ctx);
        if (methodMatcher.matches(newClass)) {
            addToMethodCalls(newClass, ctx);
            n = SearchResult.found(n);
        }
        return n;
    }

    private void addToMethodCalls(Tree tree, ExecutionContext ctx) {
        if (methodCalls != null) {
            JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
            if (javaSourceFile != null) {
                methodCalls.insertRow(ctx, new MethodCalls.Row(
                        javaSourceFile.getSourcePath().toString(),
                        tree.printTrimmed(getCursor())
                ));
            }
        }
    }

    @Incubating(since = "8.1.3")
    public static TreeVisitor<?, ExecutionContext> createVisitor(JavaTypeMethodMatcher methodMatcher, @Nullable MethodCalls methodCalls) {
        return Preconditions.check(new UsesMethod<>(methodMatcher), new FindMethodsVisitor(methodMatcher, methodCalls));
    }

    @Incubating(since = "8.1.3")
    public static TreeVisitor<?, ExecutionContext> createVisitor(JavaTypeMethodMatcher methodMatcher) {
        return createVisitor(methodMatcher, null);
    }
}
