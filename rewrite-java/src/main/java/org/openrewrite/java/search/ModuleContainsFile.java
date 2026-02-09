/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleContainsFile extends ScanningRecipe<ModuleContainsFile.Accumulator> {

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Module contains file";

    String description = "Intended to be used primarily as a precondition for other recipes, this recipe checks if a module " +
               "contains a specific file or files matching a pattern. Only files belonging to modules containing the " +
               "specified file are marked with a `SearchResult` marker. This is more specific than `RepositoryContainsFile` " +
               "which marks all files in the repository if any file matches.";

    Duration estimatedEffortPerOccurrence = Duration.ZERO;

    public static class Accumulator {
        Set<JavaProject> modulesWithMatchingFile = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                // Skip if this module is already known to contain the file
                if (sourceFile.getMarkers().findFirst(JavaProject.class)
                        .filter(acc.modulesWithMatchingFile::contains)
                        .isPresent()) {
                    return tree;
                }
                Tree t = new FindSourceFiles(filePattern).getVisitor().visit(tree, ctx);
                if (t != tree) {
                    sourceFile.getMarkers().findFirst(JavaProject.class)
                            .ifPresent(acc.modulesWithMatchingFile::add);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.modulesWithMatchingFile.isEmpty()) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getMarkers().findFirst(SearchResult.class).isPresent()) {
                    return tree;
                }
                return sourceFile.getMarkers().findFirst(JavaProject.class)
                        .filter(acc.modulesWithMatchingFile::contains)
                        .<SourceFile>map(jp -> sourceFile.withMarkers(sourceFile.getMarkers()
                                .add(new SearchResult(Tree.randomId(), "Module contains file matching pattern: " + filePattern))))
                        .orElse(sourceFile);
            }
        };
    }
}
