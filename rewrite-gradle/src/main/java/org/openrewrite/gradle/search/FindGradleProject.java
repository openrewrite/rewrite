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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindGradleProject extends Recipe {

    @Option(displayName = "Search criteria",
            description = "Whether to identify gradle projects by source file name or the presence of a marker",
            valid = {"File", "Marker"},
            example = "Marker")
    @Nullable
    SearchCriteria searchCriteria;

    @Override
    public String getDisplayName() {
        return "Find Gradle projects";
    }

    @Override
    public String getDescription() {
        return "Gradle projects are those with `build.gradle` or `build.gradle.kts` files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (searchCriteria == SearchCriteria.Marker) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext ctx) {
                    stopAfterPreVisit();
                    if (tree instanceof JavaSourceFile) {
                        JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                        if (cu.getMarkers().findFirst(GradleProject.class).isPresent()) {
                            return SearchResult.found(cu);
                        }
                    }
                    return tree;
                }
            };
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    stopAfterPreVisit();
                    SourceFile sourceFile = (SourceFile) tree;
                    if (sourceFile.getSourcePath().endsWith(Paths.get("build.gradle")) ||
                        sourceFile.getSourcePath().endsWith(Paths.get("build.gradle.kts"))) {
                        return SearchResult.found(sourceFile);
                    }
                }
                return tree;
            }
        };
    }

    public enum SearchCriteria {
        File,
        Marker
    }
}
