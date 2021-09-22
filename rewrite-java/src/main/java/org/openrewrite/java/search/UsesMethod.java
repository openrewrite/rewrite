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
import org.openrewrite.java.tree.JavaType;

import java.util.Set;

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
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        Set<JavaType> types = cu.getTypesInUse();
        for (JavaType type : types) {
            if (type instanceof JavaType.Method) {
                if(methodMatcher.matches(type)) {
                    return cu.withMarkers(cu.getMarkers().searchResult());
                }
            }
        }
        return cu;
    }
}
