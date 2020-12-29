/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.valid;
import static org.openrewrite.internal.StreamUtils.distinctBy;

public class OrderImports extends JavaIsoRefactorVisitor {
    // VisibleForTesting
    @Nullable
    Layout importLayout;

    private boolean removeUnused = true;

    @JsonIgnore
    public void setLayout(Layout importLayout) {
        this.importLayout = importLayout;
    }

    public void setRemoveUnused(boolean removeUnused) {
        this.removeUnused = removeUnused;
    }

    /**
     * @return The default import ordering of IntelliJ IDEA.
     */
    public static OrderImports.Layout intellij() {
        return Layout.builder(5, 3)
                .importAllOthers()
                .blankLine()
                .importPackage("javax.*")
                .importPackage("java.*")
                .blankLine()
                .importStaticAllOthers()
                .build();
    }

    @Override
    public Validated validate() {
        return importLayout == null ?
                Validated.none() :
                importLayout.validate();
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        List<J.Import> orderedImports = new ArrayList<>();

        if (importLayout == null) {
            importLayout = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .map(ImportLayoutStyle::orderImportLayout)
                    .orElse(intellij());
        }

        List<Layout.Block> blocks = importLayout.blocks;
        blocks.forEach(Layout.Block::reset);
        assert (blocks.stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && ((Layout.Block.AllOthers) it).statik))
                : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
        assert (blocks.stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && !((Layout.Block.AllOthers) it).statik))
                : "There must be at least one block that accepts all non-static imports, but no such block was found in the specified layout";

        // Divide the blocks into those that accept imports from any package ("catchalls") and those that accept imports from only specific packages
        Map<Boolean, List<Layout.Block>> blockGroups = blocks.stream()
                .collect(Collectors.partitioningBy(block -> block instanceof Layout.Block.AllOthers));
        List<Layout.Block> blocksNoCatchalls = blockGroups.get(false);
        List<Layout.Block> blocksOnlyCatchalls = blockGroups.get(true);

        // Allocate imports to blocks, preferring to put imports into non-catchall blocks
        for(J.Import anImport: cu.getImports()) {
            boolean accepted = false;
            for(Layout.Block block : blocksNoCatchalls) {
                if(block.accept(anImport)) {
                    accepted = true;
                    break;
                }
            }
            if(!accepted) {
                for(Layout.Block block : blocksOnlyCatchalls) {
                    if(block.accept(anImport)) {
                        accepted = true;
                        break;
                    }
                }
            }
            assert accepted : "Every import must be accepted by at least one block, but this import was not: " + anImport.printTrimmed();
        }

        int importIndex = 0;
        String extraLineSpace = "";
        for (Layout.Block block : blocks) {
            if (block instanceof Layout.Block.BlankLines) {
                extraLineSpace = "";
                for (int i = 0; i < ((Layout.Block.BlankLines) block).count; i++) {
                    //noinspection StringConcatenationInLoop
                    extraLineSpace += "\n";
                }
            } else {
                for (J.Import orderedImport : block.orderedImports()) {
                    String prefix = importIndex == 0 ? cu.getImports().get(0).getPrefix() :
                            extraLineSpace + "\n";

                    if (!orderedImport.getPrefix().equals(prefix)) {
                        orderedImports.add(orderedImport.withPrefix(prefix));
                    } else {
                        orderedImports.add(orderedImport);
                    }
                    extraLineSpace = "";
                    importIndex++;
                }
            }
        }

        if (removeUnused) {
            andThen(new RemoveUnusedImports(
                    importLayout.classCountToUseStarImport,
                    importLayout.nameCountToUseStarImport));
        }

        if (orderedImports.size() != cu.getImports().size()) {
            return cu.withImports(orderedImports);
        }

        for (int i = 0; i < orderedImports.size(); i++) {
            if (orderedImports.get(i) != cu.getImports().get(i)) {
                return cu.withImports(orderedImports);
            }
        }

        return cu;
    }
}
