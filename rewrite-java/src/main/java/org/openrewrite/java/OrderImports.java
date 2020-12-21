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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.Validated.valid;
import static org.openrewrite.java.style.ImportLayoutStyle.Layout;
import static org.openrewrite.java.style.ImportLayoutStyle.Layout.Block;

/**
 * This visitor will group and order the imports for a compilation unit using the rules defined by a {@link ImportLayoutStyle}.
 * This layout style can either be set via a style element on the compilation unit or explicitly defined via the layout
 * attribute on this visitor. If a style has not been defined, this visitor will use the default import layout style that
 * is modelled after IntelliJ's default import settings.
 * <P><P>
 * The @{link {@link OrderImports#setRemoveUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
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

    @Override
    public Validated validate() {
        return importLayout == null ? Validated.none():
                valid("layout", importLayout.getBlocks().stream()
                        .filter(b -> b instanceof Block.AllOthers && !((Block.AllOthers) b).isStatic())
                        .count() == 1);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        List<J.Import> orderedImports = new ArrayList<>();

        if (importLayout == null) {
            importLayout = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .map(ImportLayoutStyle::getLayout)
                    .orElse(ImportLayoutStyle.getDefaultImportLayout());
        }

        List<Block> blocks = importLayout.getBlocks();
        blocks.forEach(Block::reset);
        assert (blocks.stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && ((Layout.Block.AllOthers) it).isStatic()))
                : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
        assert (blocks.stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && !((Layout.Block.AllOthers) it).isStatic()))
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
        for (Block block : blocks) {
            if (block instanceof Block.BlankLines) {
                extraLineSpace = "";
                for (int i = 0; i < ((Block.BlankLines) block).getCount(); i++) {
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
            andThen(new RemoveUnusedImports());
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
