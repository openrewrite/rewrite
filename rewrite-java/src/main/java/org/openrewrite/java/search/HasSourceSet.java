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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasSourceSet extends Recipe {
    @Option(displayName = "Source set",
            description = "The source set to search for.",
            example = "main")
    String sourceSet;

    @Override
    public String getDisplayName() {
        return "Find files in a source set";
    }

    @Override
    public String getDescription() {
        return "Source sets are a way to organize your source code into logical groups. " +
               "For example, Java projects commonly have a `main` source set for application code and " +
               "a `test` source set for test code. This recipe will find all files in a given source set.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext p) {
                if (cu.getMarkers().findFirst(JavaSourceSet.class)
                        .filter(s -> s.getName().equals(sourceSet))
                        .isPresent()) {
                    return SearchResult.found(cu);
                }
                return cu;
            }
        };
    }
}
