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
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.valid;

public class OrderImports extends JavaIsoRefactorVisitor {
    // VisibleForTesting
    final static Comparator<J.Import> IMPORT_SORTING = (i1, i2) -> {
        String[] import1 = i1.getQualid().printTrimmed().split("\\.");
        String[] import2 = i2.getQualid().printTrimmed().split("\\.");

        for (int i = 0; i < Math.min(import1.length, import2.length); i++) {
            int diff = import1[i].compareTo(import2[i]);
            if (diff != 0) {
                return diff;
            }
        }

        if (import1.length == import2.length) {
            return 0;
        }

        return import1.length > import2.length ? 1 : -1;
    };

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
            importLayout = cu.getStyle(ImportLayoutStyle.class)
                    .map(ImportLayoutStyle::orderImportLayout)
                    .orElse(intellij());
        }

        List<Layout.Block> blocks = importLayout.blocks;
        blocks.forEach(Layout.Block::reset);
        assert (blocks.stream().filter( it -> it instanceof Layout.Block.AllOthers && ((Layout.Block.AllOthers) it).statik).findAny().isPresent())
                : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
        assert (blocks.stream().filter( it -> it instanceof Layout.Block.AllOthers && !((Layout.Block.AllOthers) it).statik).findAny().isPresent())
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

    public static class Layout {
        private final List<Block> blocks;
        private final int classCountToUseStarImport;
        private final int nameCountToUseStarImport;

        Layout(List<Block> blocks, int classCountToUseStarImport, int nameCountToUseStarImport) {
            this.blocks = blocks;
            this.classCountToUseStarImport = classCountToUseStarImport;
            this.nameCountToUseStarImport = nameCountToUseStarImport;
        }

        public List<Block> getBlocks() {
            return blocks;
        }

        public int getClassCountToUseStarImport() {
            return classCountToUseStarImport;
        }

        public int getNameCountToUseStarImport() {
            return nameCountToUseStarImport;
        }

        public interface Block {
            boolean accept(J.Import anImport);

            void reset();

            /**
             * @return Imports belonging to this block, folded appropriately.
             */
            List<J.Import> orderedImports();

            class BlankLines implements Block {
                private int count = 1;

                @Override
                public boolean accept(J.Import anImport) {
                    return false;
                }

                @Override
                public void reset() {
                }

                @Override
                public List<J.Import> orderedImports() {
                    return emptyList();
                }
            }

            class ImportPackage implements Block {
                private final List<J.Import> imports = new ArrayList<>();

                private final boolean statik;
                private final Pattern packageWildcard;
                private final int classCountToUseStarImport;
                private final int nameCountToUseStarImport;

                public ImportPackage(boolean statik, String packageWildcard, boolean withSubpackages,
                                     int classCountToUseStarImport, int nameCountToUseStarImport) {
                    this.statik = statik;
                    this.classCountToUseStarImport = classCountToUseStarImport;
                    this.nameCountToUseStarImport = nameCountToUseStarImport;
                    this.packageWildcard = Pattern.compile(packageWildcard
                            .replace(".", "\\.")
                            .replace("*", withSubpackages ? ".+" : "[^.]+"));
                }

                @Override
                public boolean accept(J.Import anImport) {
                    if (anImport.isStatic() == statik &&
                            packageWildcard.matcher(anImport.getQualid().printTrimmed()).matches()) {
                        imports.add(anImport);
                        return true;
                    }
                    return false;
                }

                @Override
                public void reset() {
                    imports.clear();
                }

                @Override
                public List<J.Import> orderedImports() {
                    Map<String, List<J.Import>> groupedImports = imports
                            .stream()
                            .sorted(IMPORT_SORTING)
                            .collect(groupingBy(
                                    anImport -> {
                                        if (anImport.isStatic()) {
                                            return anImport.getTypeName();
                                        } else {
                                            return anImport.getPackageName();
                                        }
                                    },
                                    LinkedHashMap::new, // Use an ordered map to preserve sorting
                                    Collectors.toList()
                            ));

                    List<J.Import> orderedStarredImports = groupedImports.values().stream()
                            .flatMap(importGroup -> {
                                J.Import toStar = importGroup.get(0);
                                boolean statik = toStar.isStatic();
                                int threshold = statik ? nameCountToUseStarImport : classCountToUseStarImport;
                                boolean starImportExists = importGroup.stream().anyMatch(it -> it.getQualid().getSimpleName().equals("*"));
                                if(importGroup.size() >= threshold || (starImportExists && importGroup.size() > 1)) {
                                    return Stream.of(toStar.withQualid(toStar.getQualid().withName(toStar.getQualid()
                                            .getName().withName("*"))));
                                } else {
                                    return importGroup.stream()
                                            .filter(Block.distinctBy(Tree::printTrimmed));
                                }
                            }).collect(toList());

                    return orderedStarredImports;
                }
            }
            // Returns a predicate suitable for use with stream().filter() that will result in a set filtered to
            // contain only items that are distinct as evaluated by keyFunc
            static <T> Predicate<T> distinctBy(Function<? super T, ?> keyFunc) {
                Set<Object> seen = new HashSet<>();
                return t -> {
                    Object it = keyFunc.apply(t);
                    boolean alreadySeen = seen.contains(it);
                    seen.add(it);
                    return !alreadySeen;
                };
            }

            class AllOthers extends ImportPackage {
                private final boolean statik;
                private Collection<ImportPackage> packageImports = emptyList();

                public AllOthers(boolean statik, int classCountToUseStarImport, int nameCountToUseStarImport) {
                    super(statik, "*", true,
                            classCountToUseStarImport, nameCountToUseStarImport);
                    this.statik = statik;
                }

                public void setPackageImports(Collection<ImportPackage> packageImports) {
                    this.packageImports = packageImports;
                }

                public boolean isStatic() {
                    return statik;
                }

                @Override
                public boolean accept(J.Import anImport) {
                    if (packageImports.stream().noneMatch(pi -> pi.accept(anImport))) {
                        super.accept(anImport);
                    }
                    return anImport.isStatic() == statik;
                }
            }
        }

        public Validated validate() {
            return valid("layout", blocks.stream()
                    .filter(b -> b instanceof Block.AllOthers && !((Block.AllOthers) b).isStatic())
                    .count() == 1);
        }

        public static Builder builder(int classCountToUseStarImport,
                                      int nameCountToUseStarImport) {
            return new Builder(classCountToUseStarImport, nameCountToUseStarImport);
        }

        public static class Builder {
            private final List<Block> blocks = new ArrayList<>();
            private final int classCountToUseStarImport;
            private final int nameCountToUseStarImport;

            public Builder(int classCountToUseStarImport, int nameCountToUseStarImport) {
                this.classCountToUseStarImport = classCountToUseStarImport;
                this.nameCountToUseStarImport = nameCountToUseStarImport;
            }

            public Builder importAllOthers() {
                blocks.add(new Block.AllOthers(false, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            public Builder importStaticAllOthers() {
                blocks.add(new Block.AllOthers(true, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            public Builder blankLine() {
                if (!blocks.isEmpty() &&
                        blocks.get(blocks.size() - 1) instanceof Block.BlankLines) {
                    ((Block.BlankLines) blocks.get(blocks.size() - 1)).count++;
                } else {
                    blocks.add(new Block.BlankLines());
                }
                return this;
            }

            public Builder importPackage(String packageWildcard, boolean withSubpackages) {
                blocks.add(new Block.ImportPackage(false, packageWildcard, withSubpackages,
                        classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            public Builder importPackage(String packageWildcard) {
                return importPackage(packageWildcard, true);
            }

            public Builder staticImportPackage(String packageWildcard, boolean withSubpackages) {
                blocks.add(new Block.ImportPackage(true, packageWildcard, withSubpackages,
                        classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            public Builder staticImportPackage(String packageWildcard) {
                return staticImportPackage(packageWildcard, true);
            }

            public Layout build() {
                for (Block block : blocks) {
                    if (block instanceof Block.AllOthers) {
                        ((Block.AllOthers) block).setPackageImports(blocks.stream()
                                .filter(b -> b.getClass().equals(Block.ImportPackage.class))
                                .map(Block.ImportPackage.class::cast)
                                .collect(toList()));
                    }
                }

                return new Layout(blocks, classCountToUseStarImport, nameCountToUseStarImport);
            }
        }
    }
}
