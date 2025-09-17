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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.UUID;

import static org.openrewrite.Tree.randomId;

public class UsesMethod<P> extends JavaIsoVisitor<P> {
    private final String methodPattern;

    @Getter
    private final MethodMatcher methodMatcher;

    public UsesMethod(String methodPattern) {
        this(new MethodMatcher(methodPattern), methodPattern);
    }

    public UsesMethod(String methodPattern, boolean matchOverrides) {
        this(new MethodMatcher(methodPattern, matchOverrides), methodPattern);
    }

    public UsesMethod(String methodPattern, @Nullable Boolean matchOverrides) {
        this(new MethodMatcher(methodPattern, Boolean.TRUE.equals(matchOverrides)), methodPattern);
    }

    public UsesMethod(MethodMatcher methodMatcher) {
        this(methodMatcher, methodMatcher.toString());
    }

    private UsesMethod(MethodMatcher methodMatcher, String methodPattern) {
        this.methodMatcher = methodMatcher;
        this.methodPattern = methodPattern;
    }

    @Override
    public J preVisit(@NonNull J tree, P p) {
        stopAfterPreVisit();
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            for (JavaType.Method type : cu.getTypesInUse().getUsedMethods()) {
                if (methodMatcher.matches(type)) {
                    return found(cu);
                }
            }
        }
        return tree;
    }

    private <J2 extends J> J2 found(J2 j) {
        // also adding a `SearchResult` marker to get a visible diff
        return SearchResult.found(j.withMarkers(j.getMarkers()
                .compute(new MethodMatch(randomId(), methodPattern), (s1, s2) -> s1 == null ? s2 : s1)));
    }

    @Value
    @With
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class MethodMatch implements Marker {
        UUID id;

        @EqualsAndHashCode.Include
        String methodMatcher;
    }
}
