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

import com.fasterxml.jackson.annotation.*;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaStyle;
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
 * A Java Style to define how imports are grouped and ordered. Additionally, this style provides configuration to dictate
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

    //The layout is maintained as a map for serialization.
    private Map<String, Object> layout;

    private Layout.Builder layoutBuilder;
    private int classCountToUseStarImport = 5;
    private int nameCountToUseStarImport = 3;

    public ImportLayoutStyle() {
    }

    @JsonIgnore
    public int getClassCountToUseStarImport() {
        return classCountToUseStarImport;
    }

    @JsonIgnore
    public int getNameCountToUseStarImport() {
        return nameCountToUseStarImport;
    }

    /**
     * {@link ImportLayoutStyle}
     * @param layout The layout is specified as a loosely-typed map that is typically populated using the declarative yml syntax.
     */
    @JsonProperty("layout")
    public void setLayout(Map<String, Object> layout) {

        this.layout = layout;
        this.classCountToUseStarImport = (Integer) layout.getOrDefault("classCountToUseStarImport", 5);
        this.nameCountToUseStarImport = (Integer) layout.getOrDefault("nameCountToUseStarImport", 3);

        Layout.Builder builder = new Layout.Builder(this.classCountToUseStarImport, this.nameCountToUseStarImport);

        //noinspection unchecked
        for (String block : (List<String>) layout.get("blocks")) {
            block = block.trim();
            if (block.equals("<blank line>")) {
                builder.blankLine();
            } else if (block.startsWith("import ")) {
                block = block.substring("import ".length());
                boolean statik = false;
                if (block.startsWith("static")) {
                    statik = true;
                    block = block.substring("static ".length());
                }
                if (block.equals("all other imports")) {
                    if (statik) {
                        builder.importStaticAllOthers();
                    } else {
                        builder.importAllOthers();
                    }
                } else {
                    if (statik) {
                        builder.staticImportPackage(block);
                    } else {
                        builder.importPackage(block);
                    }
                }
            } else {
                throw new IllegalArgumentException("Syntax error in layout block [" + block + "]");
            }
        }
        this.layoutBuilder = builder;
    }

    public Map<String, Object> getLayout() {
        return Collections.unmodifiableMap(layout);
    }

    /**
     * This method will order and group a list of imports producing a new list that conforms to the rules defined
     * by the import layout style.
     *
     * @param originalImports A list of potentially unordered imports.
     * @return A list of imports that are grouped and ordered.
     */
    public @NonNull List<J.Import> orderImports(@NonNull List<J.Import> originalImports) {

        if (layoutBuilder == null) {
            //If the builder has not been defined, default to IntelliJ's import settings.
            layoutBuilder = new Layout.Builder(5, 3)
                    .importAllOthers()
                    .blankLine()
                    .importPackage("javax.*")
                    .importPackage("java.*")
                    .blankLine()
                    .importStaticAllOthers();
        }
        Layout layout = layoutBuilder.build();
        List<J.Import> orderedImports = new ArrayList<>();
        assert (layout.getBlocks().stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && ((Layout.Block.AllOthers) it).isStatic()))
                : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
        assert (layout.getBlocks().stream().anyMatch(it -> it instanceof Layout.Block.AllOthers && !((Layout.Block.AllOthers) it).isStatic()))
                : "There must be at least one block that accepts all non-static imports, but no such block was found in the specified layout";

        // Divide the blocks into those that accept imports from any package ("catchalls") and those that accept imports from only specific packages
        Map<Boolean, List<Layout.Block>> blockGroups = layout.getBlocks().stream()
                .collect(Collectors.partitioningBy(block -> block instanceof Layout.Block.AllOthers));
        List<Layout.Block> blocksNoCatchalls = blockGroups.get(false);
        List<Layout.Block> blocksOnlyCatchalls = blockGroups.get(true);

        // Allocate imports to blocks, preferring to put imports into non-catchall blocks
        for(J.Import anImport: originalImports) {
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
        for (Layout.Block block : layout.getBlocks()) {
            if (block instanceof Layout.Block.BlankLines) {
                extraLineSpace = "";
                for (int i = 0; i < ((Layout.Block.BlankLines) block).getCount(); i++) {
                    //noinspection StringConcatenationInLoop
                    extraLineSpace += "\n";
                }
            } else {
                for (J.Import orderedImport : block.orderedImports()) {
                    String prefix = importIndex == 0 ? originalImports.get(0).getPrefix() :
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
        return orderedImports;
    }

    /**
     * @return Returns a layout style that is modeled after IntelliJ's default import settings.
     */
    public static ImportLayoutStyle getDefaultImportLayoutStyle() {
        return new ImportLayoutStyle();
    }

    /**
     * A method to create an import layout style programatically using the same block syntax as that of the declartive
     * approach. See {@link ImportLayoutStyle}
     * <P><P>Block Syntax:<P>
     *
     * <PRE>
     *      "import java.*"                   - All imports in the java package or a subpackage of java
     *      "import static org.assertj.*"     - All static imports in the "org.assertj" package or subpacakge.
     *      "&lt;blank line&gt;"                    - A blank line separator
     *      "import all other imports"        - All other imports
     *      "import static all other imports" - All other static imports
     * </PRE>
     * @param classCountToUseStarImport How many imports from the same package must be present before they should be collapsed into a star import.
     * @param nameCountToUseStarImport How many static imports from the same type must be present before they should be collapsed into a static star import.
     * @param blocks An ordered list of import groupings which define exactly how imports should be organized within a compilation unit
     * @return ImportLayoutStyle
     */
    public static @NonNull ImportLayoutStyle layout(int classCountToUseStarImport, int nameCountToUseStarImport, String... blocks) {
        Map<String, Object> settings = new HashMap<>(3);
        settings.put("classCountToUseStarImport", classCountToUseStarImport);
        settings.put("nameCountToUseStarImport", nameCountToUseStarImport);
        settings.put("blocks", Arrays.asList(blocks));
        ImportLayoutStyle style = new ImportLayoutStyle();
        style.setLayout(settings);
        return style;
    }

    /**
     * A layout is a stateful structure that can be used to sort and group a set of J.Imports.
     */
    private static class Layout {

        private final List<Block> blocks;

        private Layout(List<Block> blocks) {
            this.blocks = blocks==null?Collections.emptyList():blocks;
        }

        private List<Block> getBlocks() {
            return blocks;
        }


        /**
         * A block represents a grouping of imports based on matching rules. The block provides a mechanism for matching
         * and storing J.Imports that belong to the block.
         */
        private interface Block {

            /**
             * This method will determine if the passed in import is a match for the rules defined on the block. If the
             * import is matched, it will be internally stored in the block.
             *
             * @param anImport The import to be compared against the block's matching rules.
             * @return true if the import was a match (and was stored within the block)
             */
            boolean accept(J.Import anImport);

            /**
             * @return Imports belonging to this block, folded appropriately.
             */
            List<J.Import> orderedImports();

            /**
             * A specialized block implementation to act as a blank line separator between import groupings.
             */
            class BlankLines implements Block {
                private int count = 1;

                private int getCount() {
                    return count;
                }

                @Override
                public boolean accept(J.Import anImport) {
                    return false;
                }

                @Override
                public List<J.Import> orderedImports() {
                    return emptyList();
                }
            }

            class ImportPackage implements Layout.Block {

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

        private static class Builder {
            private final List<Block> blocks = new ArrayList<>();
            private final int classCountToUseStarImport;
            private final int nameCountToUseStarImport;

            private Builder(int classCountToUseStarImport, int nameCountToUseStarImport) {
                this.classCountToUseStarImport = classCountToUseStarImport;
                this.nameCountToUseStarImport = nameCountToUseStarImport;
            }

            private Builder importAllOthers() {
                blocks.add(new Block.AllOthers(false,classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            private Builder importStaticAllOthers() {
                blocks.add(new Block.AllOthers(true, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            private Builder blankLine() {
                if (!blocks.isEmpty() &&
                        blocks.get(blocks.size() - 1) instanceof Block.BlankLines) {
                    ((Block.BlankLines) blocks.get(blocks.size() - 1)).count++;
                } else {
                    blocks.add(new Block.BlankLines());
                }
                return this;
            }

            private Builder importPackage(String packageWildcard, boolean withSubpackages) {
                blocks.add(new Block.ImportPackage(false, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            private Builder importPackage(String packageWildcard) {
                return importPackage(packageWildcard, true);
            }

            private Builder staticImportPackage(String packageWildcard, boolean withSubpackages) {
                blocks.add(new Block.ImportPackage(true, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
                return this;
            }

            private Builder staticImportPackage(String packageWildcard) {
                return staticImportPackage(packageWildcard, true);
            }

            private Layout build() {
                for (Block block : blocks) {
                    if (block instanceof Block.AllOthers) {
                        ((Block.AllOthers) block).setPackageImports(blocks.stream()
                                .filter(b -> b.getClass().equals(Block.ImportPackage.class))
                                .map(Block.ImportPackage.class::cast)
                                .collect(toList()));
                    }
                }
                return new Layout(blocks);
            }
        }
    }
}
