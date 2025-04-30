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
package org.openrewrite.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Value
@EqualsAndHashCode(callSuper = false)
public class RepositoryContainsFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all." +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Repository contains file";
    }

    @Override
    public String getDescription() {
        return "Intended to be used primarily as a precondition for other recipes, this recipe checks if a repository " +
               "contains a specific file or files matching a pattern. If present all files in the repository are marked " +
               "with a `SearchResult` marker. If you want to get only the matching file as a search result, use `FindSourceFiles` instead.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ZERO;
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (acc.get()) {
                    return tree;
                }
                Tree t = new FindSourceFiles(filePattern).getVisitor().visit(tree, ctx);
                if (t != tree) {
                    acc.set(true);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean acc) {
        if (!acc.get()) {
            return TreeVisitor.noop();
        }
        //noinspection NullableProblems
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext executionContext) {
                if(tree.getMarkers().findFirst(SearchResult.class).isPresent()) {
                    return tree;
                }
                return tree.withMarkers(tree.getMarkers().add(new SearchResult(Tree.randomId(), "Repository contains file matching pattern: " + filePattern)));
            }
        };
    }
}
