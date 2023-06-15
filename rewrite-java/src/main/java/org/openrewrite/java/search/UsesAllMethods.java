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
package org.openrewrite.java.search;

import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTypeMethodMatcher;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Marks a {@link JavaSourceFile} as matching if all the passed methods are found.
 */
public class UsesAllMethods<P> extends JavaIsoVisitor<P> {
    private final List<JavaTypeMethodMatcher> methodMatchers;

    public UsesAllMethods(MethodMatcher... methodMatchers) {
        this(
                Arrays.stream(methodMatchers)
                        .map(JavaTypeMethodMatcher::fromMethodMatcher)
                        .collect(Collectors.toList())
        );
    }

    @Incubating(since = "8.1.3")
    public UsesAllMethods(JavaTypeMethodMatcher... methodMatchers) {
        this(Arrays.asList(methodMatchers));
    }

    @Incubating(since = "8.1.3")
    public UsesAllMethods(List<JavaTypeMethodMatcher> methodMatchers) {
        this.methodMatchers = methodMatchers;
    }


    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            List<JavaTypeMethodMatcher> unmatched = new ArrayList<>(methodMatchers);
            for (JavaType.Method type : cu.getTypesInUse().getUsedMethods()) {
                if (unmatched.removeIf(matcher -> matcher.matches(type)) && unmatched.isEmpty()) {
                    return SearchResult.found(cu);
                }
            }
        }
        return (J) tree;
    }
}
