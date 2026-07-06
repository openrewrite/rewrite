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
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
               "recomputing checksums for the whole module graph, including creating it when absent. " +
               "Useful after a dependency version change to bring `go.sum` back in sync. Requires the " +
               "`go` toolchain to be installed; otherwise `go.sum` is left unchanged.";
    }

    public static class Accumulator {
        final Map<Path, String> goModByDir = new ConcurrentHashMap<>();
        final Set<Path> goSumDirs = ConcurrentHashMap.newKeySet();
        final Map<Path, LockFileRegeneration.Result> regenByDir = new ConcurrentHashMap<>();

        LockFileRegeneration.@Nullable Result regenerate(Path dir) {
            String goMod = goModByDir.get(dir);
            if (goMod == null) {
                return null;
            }
            return regenByDir.computeIfAbsent(dir, k -> LockFileRegeneration.GO_SUM.regenerate(goMod));
        }
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
                    String fileName = fileName(sourceFile.getSourcePath());
                    if ("go.mod".equals(fileName)) {
                        acc.goModByDir.put(dirOf(sourceFile.getSourcePath()), sourceFile.printAll());
                    } else if ("go.sum".equals(fileName)) {
                        acc.goSumDirs.add(dirOf(sourceFile.getSourcePath()));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        List<SourceFile> created = new ArrayList<>();
        for (Path dir : acc.goModByDir.keySet()) {
            if (acc.goSumDirs.contains(dir)) {
                continue;
            }
            LockFileRegeneration.Result result = acc.regenerate(dir);
            if (result == null || !result.isSuccess()) {
                continue;
            }
            String content = result.getLockFileContent();
            if (content == null || content.isEmpty()) {
                continue;
            }
            Path goSumPath = dir.resolve("go.sum");
            PlainTextParser.builder().build().parse(content)
                    .map(pt -> (SourceFile) pt.withSourcePath(goSumPath))
                    .forEach(created::add);
        }
        return created;
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
                LockFileRegeneration.Result result = acc.regenerate(dirOf(goSum.getSourcePath()));
                if (result == null) {
                    return tree;
                }
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
