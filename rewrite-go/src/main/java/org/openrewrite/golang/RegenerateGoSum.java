/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.internal.LockFileRegeneration;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Regenerates {@code go.sum} to match the sibling {@code go.mod} by running the
 * real {@code go} toolchain, e.g. after a dependency upgrade edited {@code go.mod}.
 * The checksums are produced by {@code go mod download}, so they are byte-identical
 * to what the toolchain writes; {@code go.mod} itself is left untouched.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RegenerateGoSum extends ScanningRecipe<RegenerateGoSum.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Regenerate `go.sum`";
    }

    @Override
    public String getDescription() {
        return "Regenerate a Go module's `go.sum` from its `go.mod` by running `go mod download`, " +
               "recomputing checksums for the whole module graph. Useful after a dependency version " +
               "change to bring `go.sum` back in sync. Requires the `go` toolchain to be installed; " +
               "otherwise `go.sum` is left unchanged.";
    }

    public static class Accumulator {
        final Map<Path, String> goModByDir = new HashMap<>();
        final Map<Path, LockFileRegeneration.Result> regenByDir = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    if ("go.mod".equals(fileName(sourceFile.getSourcePath()))) {
                        acc.goModByDir.put(dirOf(sourceFile.getSourcePath()), sourceFile.printAll());
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof PlainText)) {
                    return tree;
                }
                PlainText goSum = (PlainText) tree;
                if (!"go.sum".equals(fileName(goSum.getSourcePath()))) {
                    return tree;
                }
                String goMod = acc.goModByDir.get(dirOf(goSum.getSourcePath()));
                if (goMod == null) {
                    return tree;
                }

                LockFileRegeneration.Result result = acc.regenByDir.computeIfAbsent(
                        dirOf(goSum.getSourcePath()), k -> LockFileRegeneration.GO_SUM.regenerate(goMod));
                if (!result.isSuccess()) {
                    return tree;
                }

                String regenerated = result.getLockFileContent();
                if (regenerated == null || regenerated.equals(goSum.getText())) {
                    return tree;
                }
                return goSum.withText(regenerated);
            }
        };
    }

    private static @Nullable String fileName(Path path) {
        Path name = path.getFileName();
        return name == null ? null : name.toString();
    }

    private static Path dirOf(Path path) {
        Path parent = path.getParent();
        return parent == null ? Paths.get("") : parent;
    }
}
