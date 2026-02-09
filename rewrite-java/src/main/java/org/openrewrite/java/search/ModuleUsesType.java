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
public class ModuleUsesType extends ScanningRecipe<ModuleUsesType.Accumulator> {

    @Option(displayName = "Type pattern",
            description = "A fully-qualified type name, optionally with a `*` wildcard to match any type in a package " +
                          "or `..` to match types in subpackages as well.",
            example = "org.springframework..*")
    String fullyQualifiedTypeName;

    @Option(displayName = "Include implicit usages",
            description = "When enabled, implicit usages of types (such as through method return types and parameter types) " +
                          "will also be considered. Default is false.",
            required = false)
    @Nullable
    Boolean includeImplicit;

    String displayName = "Module uses type";

    String description = "Intended to be used primarily as a precondition for other recipes, this recipe checks if a module " +
               "uses a specified type. Only files belonging to modules that use the specified type are marked with a " +
               "`SearchResult` marker. This is more specific than `UsesType` which only marks the files that directly use the type.";

    Duration estimatedEffortPerOccurrence = Duration.ZERO;

    public static class Accumulator {
        Set<JavaProject> modulesUsingType = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        UsesType<ExecutionContext> usesType = new UsesType<>(fullyQualifiedTypeName, includeImplicit);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                // Skip if this module is already known to use the type
                if (sourceFile.getMarkers().findFirst(JavaProject.class)
                        .filter(acc.modulesUsingType::contains)
                        .isPresent()) {
                    return tree;
                }
                if (usesType.isAcceptable(sourceFile, ctx) && usesType.visit(tree, ctx) != tree) {
                    sourceFile.getMarkers().findFirst(JavaProject.class)
                            .ifPresent(acc.modulesUsingType::add);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.modulesUsingType.isEmpty()) {
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
                        .filter(acc.modulesUsingType::contains)
                        .<SourceFile>map(jp -> sourceFile.withMarkers(sourceFile.getMarkers()
                                .add(new SearchResult(Tree.randomId(), "Module uses type: " + fullyQualifiedTypeName))))
                        .orElse(sourceFile);
            }
        };
    }
}
