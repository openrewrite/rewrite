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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class LockDependencyVersions extends ScanningRecipe<LockDependencyVersions.Accumulator> {
    private static final String[] EMPTY = new String[0];

    @Override
    public String getDisplayName() {
        return "Create or update a gradle.lockfile";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Using dynamic dependency versions (e.g., 1.+ or [1.0,2.0)) can cause builds to break unexpectedly because the exact version of a dependency that gets resolved can change over time.\n" +
                "To ensure reproducible builds, it’s necessary to lock versions of dependencies and their transitive dependencies. " +
                "This guarantees that a build with the same inputs will always resolve to the same module versions, a process known as dependency locking.\n" +
                "This recipe creates a `gradle.lockfile` in the root of every gradle module in the project.\n" +
                "The `gradle.lockfile` contains the resolved versions of all dependencies and their transitive dependencies, ensuring that the same versions are used across different builds.\n" +
                "The dependencies used for locking are the ones that are resolved during the BUILD time of your LST. \n" +
                "If you want to update the lockfile after impacting dependencies using a recipe, either you first need to build a new LST or use the `UpdateDependencyLockFileVisitor`.";
    }

    @Value
    static class Accumulator {
        UpdateDependencyLock.DependencyState dependencyState = new UpdateDependencyLock.DependencyState();
        Set<String> lockfiles = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                        (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) ||
                        (sourceFile instanceof PlainText && sourceFile.getSourcePath().endsWith("gradle.lockfile"));
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof PlainText && ((PlainText) tree).getSourcePath().endsWith("gradle.lockfile")) {
                    acc.getLockfiles().add(((PlainText) tree).getSourcePath().toString());
                }
                return acc.dependencyState.addGradleModule(super.visit(tree, ctx));
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        return acc.dependencyState.getModules().keySet().stream()
                .flatMap(lockfilePath ->
                        PlainTextParser.builder().build()
                                .parse("# This is a Gradle generated file for dependency locking.\n" +
                                        "# Manual edits can break the build and are not advised.\n" +
                                        "# This file is expected to be part of source control.\n" +
                                        "empty=")
                                .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(lockfilePath)))
                                .filter(brandNewFile -> !acc.lockfiles.contains(brandNewFile.getSourcePath().toString())))
                .collect(Collectors.toList());
    }

    @Override
    public UpdateDependencyLock getVisitor(Accumulator acc) {
        return new UpdateDependencyLock(acc.dependencyState);
    }

}
