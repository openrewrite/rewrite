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
import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.UUID;

public class UsesMethod<P> extends JavaIsoVisitor<P> {

    /**
     * Whether {@link TypesInUse} on the runtime classpath exposes {@link TypesInUse#hasMethodUse}.
     * Recipes can be deployed against an older parent-loaded {@code rewrite-java} that lacks the
     * method; probing once at class load lets us fall back to direct iteration without
     * {@link NoSuchMethodError}.
     */
    private static final boolean TYPES_IN_USE_HAS_METHOD_USE = probeTypesInUseHasMethodUse();

    private static boolean probeTypesInUseHasMethodUse() {
        try {
            TypesInUse.class.getMethod("hasMethodUse", MethodMatcher.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Getter
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
    public J preVisit(J tree, P p) {
        stopAfterPreVisit();
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            if (TYPES_IN_USE_HAS_METHOD_USE
                    ? cu.getTypesInUse().hasMethodUse(methodMatcher)
                    : legacyHasMethodUse(cu)) {
                return SearchResult.found(cu);
            }
        }
        return tree;
    }

    private boolean legacyHasMethodUse(JavaSourceFile cu) {
        for (JavaType.Method type : cu.getTypesInUse().getUsedMethods()) {
            if (methodMatcher.matches(type)) {
                return true;
            }
        }
        return false;
    }

    /// No longer added: preconditions now see a source file without its own [SearchResult] markers, so
    /// `UsesMethod` no longer needs a marker of its own to show a match. Kept so older LSTs and recipes still load.
    @Deprecated
    @Value
    @With
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class MethodMatch implements Marker {
        UUID id;

        @EqualsAndHashCode.Include
        String methodMatcher;
    }
}
