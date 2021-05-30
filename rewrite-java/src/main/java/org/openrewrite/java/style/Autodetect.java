/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.java.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Autodetect detect(List<J.CompilationUnit> cus) {
        IndentStatistics indentStatistics = new IndentStatistics();
        ImportLayoutStatistics importLayoutStatistics = new ImportLayoutStatistics();

        for (J.CompilationUnit cu : cus) {
            new FindIndentJavaVisitor().visit(cu, indentStatistics);
            new FindImportLayout().visit(cu, importLayoutStatistics);
        }

        return new Autodetect(Tree.randomId(), Arrays.asList(
                indentStatistics.getTabsAndIndentsStyle(),
                importLayoutStatistics.getImportLayoutStyle()));
    }

    private static class IndentStatistics {
        private final Map<Integer, Long> indentFrequencies = new HashMap<>();
        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            boolean useTabs = !isIndentedWithSpaces();

            Map.Entry<Integer, Long> i1 = null;
            Map.Entry<Integer, Long> i2 = null;

            for (Map.Entry<Integer, Long> sample : indentFrequencies.entrySet()) {
                if (sample.getKey() == 0) {
                    continue;
                }
                if (i1 == null || i1.getValue() < sample.getValue()) {
                    i1 = sample;
                } else if (i2 == null || i2.getValue() < sample.getValue()) {
                    i2 = sample;
                }
            }

            int indent1 = i1 == null ? 4 : i1.getKey();
            int indent2 = i2 == null ? indent1 : i2.getKey();

            int indent = Math.min(indent1, indent2);
            int continuationIndent = Math.max(indent1, indent2);

            return new TabsAndIndentsStyle(
                    useTabs,
                    useTabs ? indent : 1,
                    useTabs ? 4 : indent,
                    continuationIndent,
                    false
            );
        }
    }

    private static class FindIndentJavaVisitor extends JavaIsoVisitor<IndentStatistics> {
        @Override
        public Space visitSpace(Space space, Space.Location loc, IndentStatistics stats) {
            Integer lastIndent = getCursor().getNearestMessage("lastIndent");
            if (lastIndent == null) {
                lastIndent = 0;
            }

            String prefix = space.getWhitespace();

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            if (prefix.chars()
                    .filter(c -> {
                        takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                        return takeWhile.get();
                    })
                    .count() > 0) {
                int indent = 0;
                char[] chars = prefix.toCharArray();
                for (char c : chars) {
                    if (c == '\n' || c == '\r') {
                        indent = 0;
                        continue;
                    }
                    if (Character.isWhitespace(c)) {
                        indent++;
                    }
                }

                stats.indentFrequencies.merge(indent - lastIndent, 1L, Long::sum);
                getCursor().putMessage("lastIndent", indent);

                AtomicBoolean dropWhile = new AtomicBoolean(false);
                takeWhile.set(true);
                Map<Boolean, Long> indentTypeCounts = prefix.chars()
                        .filter(c -> {
                            dropWhile.set(dropWhile.get() || !(c == '\n' || c == '\r'));
                            return dropWhile.get();
                        })
                        .filter(c -> {
                            takeWhile.set(takeWhile.get() && Character.isWhitespace(c));
                            return takeWhile.get();
                        })
                        .mapToObj(c -> c == ' ')
                        .collect(Collectors.groupingBy(identity(), counting()));

                if (indentTypeCounts.getOrDefault(true, 0L) >= indentTypeCounts.getOrDefault(false, 0L)) {
                    stats.linesWithSpaceIndents++;
                } else {
                    stats.linesWithTabIndents++;
                }
            }

            return space;
        }
    }

    private static class ImportLayoutStatistics {
        List<List<Block>> blocksPerSourceFile = new ArrayList<>();

        enum BlockType {
            BlankLine,
            Import,
            ImportStatic
        }

        public ImportLayoutStyle getImportLayoutStyle() {
            // the simplest heuristic is just to take the single longest block sequence and
            // assume that represents the most variation in the project
            return blocksPerSourceFile.stream()
                    .max(Comparator.comparing(List::size))
                    .map(longestBlocks -> {
                        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
                        for (Block block : longestBlocks) {
                            switch (block.type) {
                                case BlankLine:
                                    builder = builder.blankLine();
                                    break;
                                case Import:
                                    assert block.pattern != null;
                                    if ("all other imports".equals(block.pattern)) {
                                        builder = builder.importAllOthers();
                                    } else {
                                        if (longestBlocks.stream().noneMatch(b -> b.type == BlockType.Import &&
                                                "all other imports".equals(b.pattern))) {
                                            builder = builder.importAllOthers().blankLine();
                                        }
                                        builder = builder.importPackage(block.pattern);
                                    }
                                    break;
                                case ImportStatic:
                                    assert block.pattern != null;
                                    if ("all other imports".equals(block.pattern)) {
                                        builder = builder.importStaticAllOthers();
                                    } else {
                                        if (longestBlocks.stream().noneMatch(b -> b.type == BlockType.ImportStatic &&
                                                "all other imports".equals(b.pattern))) {
                                            builder = builder.importStaticAllOthers().blankLine();
                                        }
                                        builder = builder.staticImportPackage(block.pattern);
                                    }
                                    break;
                            }
                        }
                        return builder.build();
                    })
                    .orElse(IntelliJ.importLayout());
        }

        static class Block {
            private final BlockType type;

            @Nullable
            private final String pattern;

            Block(BlockType type) {
                this.type = type;
                this.pattern = null;
            }

            Block(BlockType type, List<J.Import> imports) {
                this.type = type;
                this.pattern = getPattern(imports);
            }

            static String getPattern(List<J.Import> imports) {
                String longestCommonPrefix = null;

                for (J.Import anImport : imports) {
                    String pkg = anImport.getPackageName();
                    if (pkg.startsWith("java.")) {
                        return "java.*";
                    } else if (pkg.startsWith("javax.")) {
                        return "javax.*";
                    } else {
                        longestCommonPrefix = longestCommonPrefix(pkg, longestCommonPrefix);
                        if (longestCommonPrefix.isEmpty()) {
                            return "all other imports";
                        }
                    }
                }

                return longestCommonPrefix == null ? "all other imports" : longestCommonPrefix;
            }

            static String longestCommonPrefix(String pkg, @Nullable String lcp) {
                if (lcp == null) {
                    return pkg;
                }

                char[] p1 = pkg.toCharArray();
                char[] p2 = lcp.toCharArray();
                int i = 0;
                for (; i < p1.length && i < p2.length; i++) {
                    if (p1[i] != p2[i]) {
                        break;
                    }
                }
                return lcp.substring(0, i);
            }
        }
    }

    private static class FindImportLayout extends JavaIsoVisitor<ImportLayoutStatistics> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ImportLayoutStatistics importLayoutStatistics) {
            List<ImportLayoutStatistics.Block> blocks = new ArrayList<>();

            boolean staticBlock = false;
            int blockStart = 0;
            int i = 0;
            for (J.Import anImport : cu.getImports()) {
                if (anImport.getPrefix().getWhitespace().contains("\n\n")) {
                    if (i - blockStart > 0) {
                        blocks.add(new ImportLayoutStatistics.Block(
                                staticBlock ?
                                        ImportLayoutStatistics.BlockType.ImportStatic :
                                        ImportLayoutStatistics.BlockType.Import,
                                cu.getImports().subList(blockStart, i)));
                    }
                    blocks.add(new ImportLayoutStatistics.Block(ImportLayoutStatistics.BlockType.BlankLine));
                    blockStart = i;
                }
                staticBlock = anImport.isStatic();
                i++;
            }

            if (i - blockStart > 0) {
                blocks.add(new ImportLayoutStatistics.Block(
                        staticBlock ?
                                ImportLayoutStatistics.BlockType.ImportStatic :
                                ImportLayoutStatistics.BlockType.Import,
                        cu.getImports().subList(blockStart, i)));
            }

            importLayoutStatistics.blocksPerSourceFile.add(blocks);

            return cu;
        }
    }
}
