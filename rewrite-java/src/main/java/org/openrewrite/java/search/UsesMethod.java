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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

public class UsesMethod<P> extends JavaIsoVisitor<P> {
    private final MethodMatcher methodMatcher;

    public UsesMethod(String methodPattern) {
        this(new MethodMatcher(methodPattern));
    }

    public UsesMethod(String methodPattern, boolean matchOverrides) {
        this(new MethodMatcher(methodPattern, matchOverrides));
    }

    public UsesMethod(String methodPattern, @Nullable Boolean matchOverrides) {
        this(new MethodMatcher(methodPattern, Boolean.TRUE.equals(matchOverrides)));
    }

    public UsesMethod(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, P p) {
        for (JavaType.Method type : cu.getTypesInUse().getUsedMethods()) {
            if (methodMatcher.matches(type)) {
                return SearchResult.found(cu);
            }
        }
        return cu;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        if (methodMatcher.matches(method)) {
            return SearchResult.found(method);
        }
        return super.visitMethodInvocation(method, p);
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        if (methodMatcher.matches(memberRef)) {
            return SearchResult.found(memberRef);
        }
        return super.visitMemberReference(memberRef, p);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        if (methodMatcher.matches(newClass)) {
            return SearchResult.found(newClass);
        }
        return super.visitNewClass(newClass, p);
    }
}
