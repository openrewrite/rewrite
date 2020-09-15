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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.valid;

public class OrderImports extends JavaRefactorVisitor {
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
    public J visitCompilationUnit(J.CompilationUnit cu) {
        List<J.Import> orderedImports = new ArrayList<>();

        if (importLayout == null) {
            importLayout = cu.getStyle(ImportLayoutStyle.class)
                    .map(ImportLayoutStyle::orderImportLayout)
                    .orElse(intellij());
        }

        int importIndex = 0;
        String extraLineSpace = "";
        List<Layout.Block> blocks = importLayout.blocks;
        for (Layout.Block block : blocks) {
            if (block instanceof Layout.Block.BlankLines) {
                extraLineSpace = "";
                for (int i = 0; i < ((Layout.Block.BlankLines) block).count; i++) {
                    //noinspection StringConcatenationInLoop
                    extraLineSpace += "\n";
                }
            } else {
                block.reset();
                cu.getImports().forEach(block::accept);
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

                private static final J.Import SPACER = new J.Import(randomId(),
                        new J.FieldAccess(randomId(),
                                J.Ident.build(randomId(), "_SPACER", null, Formatting.EMPTY),
                                J.Ident.build(randomId(), "_SPACER", null, Formatting.EMPTY),
                                null,
                                Formatting.EMPTY),
                        false,
                        Formatting.EMPTY);

                @Override
                public List<J.Import> orderedImports() {
                    imports.sort(IMPORT_SORTING);

                    // simplifies the logic of dealing with folding the last group of imports
                    imports.add(SPACER);

                    boolean foundStar = false;
                    int consecutiveSamePackages = 0;
                    for (int i = 0; i < imports.size(); i++) {
                        consecutiveSamePackages++;
                        if (i >= 1 && !imports.get(i - 1).getPackageName().equals(imports.get(i).getPackageName())) {
                            int threshold = statik ? nameCountToUseStarImport : classCountToUseStarImport;
                            if (consecutiveSamePackages >= threshold || (consecutiveSamePackages > 1 && foundStar)) {
                                int j = i - 1;
                                for (; j > i - consecutiveSamePackages + 1; j--) {
                                    imports.remove(j);
                                }
                                J.Import toStar = imports.get(j);
                                if (!toStar.getQualid().getSimpleName().equals("*")) {
                                    imports.set(j, toStar.withQualid(toStar.getQualid().withName(toStar.getQualid()
                                            .getName().withName("*"))));
                                }
                                i = j + 1;
                            }
                            consecutiveSamePackages = 1;
                            foundStar = false;
                        }
                        if ("*".equals(imports.get(i).getQualid().getSimpleName())) {
                            foundStar = true;
                        }
                    }

                    imports.remove(SPACER);
                    return imports;
                }
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
                        return super.accept(anImport);
                    }
                    return false;
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
