/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import lombok.AllArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Removes all {@link MethodCall} matching both the
 * {@link RemoveMethodCallVisitor#methodMatcher}
 * and the
 * {@link RemoveMethodCallVisitor#argumentPredicate} for all arguments.
 * <p>
 * Only removes {@link MethodCall} where the call's return value is unused.
 */
@AllArgsConstructor
public class RemoveMethodCallVisitor<P> extends JavaIsoVisitor<P> {
    /**
     * The {@link MethodCall} to match to be removed.
     */
    private final MethodMatcher methodMatcher;
    /**
     * All arguments must match the predicate for the {@link MethodCall} to be removed.
     */
    private final BiPredicate<Integer, Expression> argumentPredicate;

    @SuppressWarnings("NullableProblems")
    @Nullable
    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        return visitMethodCall(newClass, () -> super.visitNewClass(newClass, p));
    }

    @SuppressWarnings("NullableProblems")
    @Nullable
    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        return visitMethodCall(method, () -> super.visitMethodInvocation(method, p));
    }

    @Nullable
    private <M extends MethodCall> M visitMethodCall(M methodCall, Supplier<M> visitSuper) {
        if (!methodMatcher.matches(methodCall)) {
            return visitSuper.get();
        }
        J.Block parentBlock = getCursor().firstEnclosing(J.Block.class);
        //noinspection SuspiciousMethodCalls
        if (parentBlock != null && !parentBlock.getStatements().contains(methodCall)) {
            return visitSuper.get();
        }
        // Remove the method invocation when the argumentMatcherPredicate is true for all arguments
        for (int i = 0; i < methodCall.getArguments().size(); i++) {
            if (!argumentPredicate.test(i, methodCall.getArguments().get(i))) {
                return visitSuper.get();
            }
        }
        if (methodCall.getMethodType() != null) {
            maybeRemoveImport(methodCall.getMethodType().getDeclaringType());
        }
        return null;
    }
}
