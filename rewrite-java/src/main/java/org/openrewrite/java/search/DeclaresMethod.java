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
package org.openrewrite.java.search;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

public class DeclaresMethod<P> extends JavaIsoVisitor<P> {
    private final MethodMatcher methodMatcher;

    public DeclaresMethod(String methodPattern) {
        this(methodPattern, false);
    }

    public DeclaresMethod(String methodPattern, @Nullable Boolean matchesOverrides) {
        this(new MethodMatcher(methodPattern, Boolean.TRUE.equals(matchesOverrides)));
    }

    public DeclaresMethod(String methodPattern, boolean matchesOverrides) {
        this(new MethodMatcher(methodPattern, matchesOverrides));
    }

    public DeclaresMethod(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            for (JavaType.Method method : cu.getTypesInUse().getDeclaredMethods()) {
                if (methodMatcher.matches(method)) {
                    return SearchResult.found(cu);
                }
            }
        }
        return (J) tree;
    }
}
