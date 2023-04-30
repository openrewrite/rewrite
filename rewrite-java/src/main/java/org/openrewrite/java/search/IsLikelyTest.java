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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@Incubating(since = "7.36.0")
public class IsLikelyTest extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find sources that are likely tests";
    }

    @Override
    public String getDescription() {
        return "Sources that contain indicators of being, or being exclusively for the use in tests. " +
               "This recipe is not exhaustive, but is intended to be a good starting point for finding test sources. " +
               "Looks at the source set name, and types in use; for example looks for uses of JUnit & TestNG annotations/assertions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.or(
                new HasSourceSet("test").getVisitor(),
                new HasSourceSetNameContainingTestVisitor<>(),
                new UsesType<>("org.junit..*", true), // Covers both JUnit 4 and 5
                new UsesType<>("org.testng..*", true),
                new UsesType<>("org.hamcrest..*", true),
                new UsesType<>("org.mockito..*", true),
                new UsesType<>("org.powermock..*", true),
                new UsesType<>("org.assertj..*", true),
                new UsesType<>("spock.lang..*", true)
        );
    }

    private static class HasSourceSetNameContainingTestVisitor<P> extends JavaIsoVisitor<P> {
        @Override
        public J visit(@Nullable Tree tree, P p) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                if (cu.getMarkers().findFirst(JavaSourceSet.class)
                        .filter(s -> s.getName().toLowerCase(Locale.ROOT).contains("test"))
                        .isPresent()) {
                    return SearchResult.found(cu);
                }
            }
            return (J) tree;
        }
    }
}
