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
package org.openrewrite.java.style;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.valid;
import static org.openrewrite.internal.StreamUtils.distinctBy;

public class ImportLayoutStyle implements JavaStyle {
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

    private Map<String, Object> layout;

    public Layout orderImportLayout() {
        Layout.Builder builder = Layout.builder(
                (Integer) layout.getOrDefault("classCountToUseStarImport", 5),
                (Integer) layout.getOrDefault("nameCountToUseStarImport", 3));

        //noinspection unchecked
        for (String block : (List<String>) layout.get("blocks")) {
            block = block.trim();
            if (block.equals("<blank line>")) {
                builder = builder.blankLine();
            } else if (block.startsWith("import ")) {
                block = block.substring("import ".length());
                boolean statik = false;
                if (block.startsWith("static")) {
                    statik = true;
                    block = block.substring("static ".length());
                }
                if (block.equals("all other imports")) {
                    builder = statik ?
                            builder.importStaticAllOthers() :
                            builder.importAllOthers();
                } else {
                    builder = statik ?
                            builder.staticImportPackage(block) :
                            builder.importPackage(block);
                }
            }
        }

        return builder.build();
    }

    @JsonProperty("layout")
    public void setLayout(Map<String, Object> layout) {
        this.layout = layout;
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

                    return groupedImports.values().stream()
                            .flatMap(importGroup -> {
                                J.Import toStar = importGroup.get(0);
                                boolean statik1 = toStar.isStatic();
                                int threshold = statik1 ? nameCountToUseStarImport : classCountToUseStarImport;
                                boolean starImportExists = importGroup.stream().anyMatch(it -> it.getQualid().getSimpleName().equals("*"));
                                if (importGroup.size() >= threshold || (starImportExists && importGroup.size() > 1)) {
                                    J.FieldAccess qualid = toStar.getQualid();
                                    JLeftPadded<J.Ident> name = qualid.getName();
                                    return Stream.of(toStar.withQualid(qualid.withName(name.withElem(
                                            name.getElem().withName("*")))));
                                } else {
                                    return importGroup.stream()
                                            .filter(distinctBy(Tree::printTrimmed));
                                }
                            }).collect(toList());
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
