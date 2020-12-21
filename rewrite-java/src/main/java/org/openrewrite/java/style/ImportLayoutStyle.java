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
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StreamUtils.distinctBy;

/**
 * A Java Style to define how imports are gropued and ordered. Additionally, this style provides configuration to dictate
 * how wildcard folding should be applied when multiple imports are in the same package or on the same, statically-imported
 * type.
 * <P><P>
 * The import layout consist of three properties:
 * <P><P>
 * <li>classCountToUseStarImport  - How many imports from the same package must be present before they should be collapsed into a star import. The default is 5.</li>
 * <li>nameCountToUseStarImport - How many static imports from the same type must be present before they should be collapsed into a static star import. The default is 3.</li>
 * <li>blocks - An ordered list of import groupings which define exactly how imports should be organized within a compilation unit</li>
 * <P><P>
 * Example:
 * <P><PRE>
 * ---
 * type: specs.openrewrite.org/v1beta/style
 * name: io.moderne.spring.style
 *
 * configure:
 *   org.openrewrite.java.style.ImportLayoutStyle:
 *     layout:
 *       classCountToUseStarImport: 999
 *       nameCountToUseStarImport: 999
 *       blocks:
 *         - import java.*
 *         - &lt;blank line&gt;
 *         - import javax.*
 *         - &lt;blank line&gt;
 *         - import all other imports
 *         - &lt;blank line&gt;
 *         - import org.springframework.*
 *         - &lt;blank line&gt;
 *         - import static all other imports
 * </PRE>
 */
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

    private Layout.Builder layoutBuilder;

    /**
     * {@link ImportLayoutStyle}
     * @param layout The layout is specified as a loosely-typed map that is typically populated using the declarative yml syntax.
     */
    @JsonProperty("layout")
    public void setLayout(Map<String, Object> layout) {

        Layout.Builder builder = layoutBuilder();

        builder.classCountToUseStarImport((Integer) layout.getOrDefault("classCountToUseStarImport", 5));
        builder.nameCountToUseStarImport((Integer) layout.getOrDefault("nameCountToUseStarImport", 3));

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
        this.layoutBuilder = builder;
    }

    /**
     * A layout instance can be used to collect a set of {@link J.Import} objects into
     * sorted groupings (blocks). No
     * @return A new layout instance built using the layout styles.
     */
    public Layout getLayout() {
        if (layoutBuilder == null) {
            layoutBuilder = defaultLayoutBuilder();
        }
        return layoutBuilder.build();
    }

    /**
     * @return Returns a default layout that is modeled after IntelliJ's default import settings.
     */
    public static Layout getDefaultImportLayout() {
        return defaultLayoutBuilder().build();
    }

    public static Layout.Builder layoutBuilder() {
        return new Layout.Builder();
    }

    private static Layout.Builder defaultLayoutBuilder() {
        return layoutBuilder()
                .importAllOthers()
                .blankLine()
                .importPackage("javax.*")
                .importPackage("java.*")
                .blankLine()
                .importStaticAllOthers();
    }


    public static class Layout {

        private final List<Block> blocks;
        private final int classCountToUseStarImport;
        private final int nameCountToUseStarImport;

        private Layout(List<Block> blocks, int classCountToUseStarImport, int nameCountToUseStarImport) {
            this.blocks = blocks==null?Collections.emptyList():blocks;
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

        public void reset() {
            blocks.forEach(Block::reset);
        }

        /**
         * A block represents a grouping of imports based on matching rules. The block provides a mechanism for matching
         * and storing J.Imports that belong to the block.
         *
         * The internal state of a block can be cleared by calling "reset"
         */
        public interface Block {

            /**
             * This method will determine if the passed in import is a match for the rules defined on the block. If the
             * import is matched, it will be internally stored in the class.
             *
             * @param anImport The import to be compared against the block's matching rules.
             * @return true if the import was a match (and was stored within the block)
             */
            boolean accept(J.Import anImport);

            /**
             * Reset the internal state of the block, this allows the same instance of a layout to be reused across
             * compilation units.
             */
            void reset();

            /**
             * @return Imports belonging to this block, folded appropriately.
             */
            List<J.Import> orderedImports();

            class BlankLines implements Block {
                private int count = 1;

                public int getCount() {
                    return count;
                }
                public boolean matches(J.Import anImport) {
                    return false;
                }
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

            class ImportPackage implements Layout.Block {
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
                                if(importGroup.size() >= threshold || (starImportExists && importGroup.size() > 1)) {
                                    return Stream.of(toStar.withQualid(toStar.getQualid().withName(toStar.getQualid()
                                            .getName().withName("*"))));
                                } else {
                                    return importGroup.stream()
                                            .filter(distinctBy(Tree::printTrimmed));
                                }
                            }).collect(toList());
                }
            }

            class AllOthers extends Block.ImportPackage {
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

        public static class Builder {
            private final List<Block> blocks = new ArrayList<>();
            private int classCountToUseStarImport = 5;
            private int nameCountToUseStarImport = 3;

            public Builder() {
            }

            public Builder classCountToUseStarImport(Integer classCount) {
                this.classCountToUseStarImport = classCount;
                return this;
            }

            public Builder nameCountToUseStarImport(Integer nameCount) {
                this.nameCountToUseStarImport = nameCount;
                return this;
            }

            public Builder importAllOthers() {
                blocks.add(new Block.AllOthers(false,classCountToUseStarImport, nameCountToUseStarImport));
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
                blocks.add(new Block.ImportPackage(false, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            public Builder importPackage(String packageWildcard) {
                return importPackage(packageWildcard, true);
            }

            public Builder staticImportPackage(String packageWildcard, boolean withSubpackages) {
                blocks.add(new Block.ImportPackage(true, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
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
