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
import lombok.EqualsAndHashCode;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
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
        SpacesStatistics spacesStatistics = new SpacesStatistics();

        for (J.CompilationUnit cu : cus) {
            new FindIndentJavaVisitor().visit(cu, indentStatistics);
            new FindImportLayout().visit(cu, importLayoutStatistics);
            new FindSpacesStyle().visit(cu, spacesStatistics);
        }

        return new Autodetect(Tree.randomId(), Arrays.asList(
                indentStatistics.getTabsAndIndentsStyle(),
                importLayoutStatistics.getImportLayoutStyle(),
                spacesStatistics.getSpacesStyle()));
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
        int minimumFoldedImports = Integer.MAX_VALUE;
        int minimumFoldedStaticImports = Integer.MAX_VALUE;

        enum BlockType {
            Import,
            ImportStatic
        }

        public ImportLayoutStyle getImportLayoutStyle() {
            // the simplest heuristic is just to take the single longest block sequence and
            // assume that represents the most variation in the project
            return blocksPerSourceFile.stream()
                    .max(Comparator
                            .<List<Block>, Integer>comparing(List::size)
                            .thenComparing(blocks -> blocks.stream()
                                    .filter(b -> "all other imports".equals(b.pattern))
                                    .count()
                            )
                    )
                    .map(longestBlocks -> {
                        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
                        boolean importAllOthers = false;
                        boolean importStaticAllOthers = false;
                        int i = 0;
                        for (Block block : longestBlocks) {
                            if (i++ != 0) {
                                builder = builder.blankLine();
                            }

                            switch (block.type) {
                                case Import:
                                    assert block.pattern != null;
                                    if ("all other imports".equals(block.pattern)) {
                                        importAllOthers = true;
                                        builder = builder.importAllOthers();
                                    } else {
                                        if (longestBlocks.stream().noneMatch(b -> b.type == BlockType.Import &&
                                                "all other imports".equals(b.pattern))) {
                                            importAllOthers = true;
                                            builder = builder.importAllOthers().blankLine();
                                        }
                                        builder = builder.importPackage(block.pattern);
                                    }
                                    break;
                                case ImportStatic:
                                    assert block.pattern != null;
                                    if ("all other imports".equals(block.pattern)) {
                                        importStaticAllOthers = true;
                                        builder = builder.importStaticAllOthers();
                                    } else {
                                        if (longestBlocks.stream().noneMatch(b -> b.type == BlockType.ImportStatic &&
                                                "all other imports".equals(b.pattern))) {
                                            importStaticAllOthers = true;
                                            builder = builder.importStaticAllOthers().blankLine();
                                        }
                                        builder = builder.staticImportPackage(block.pattern);
                                    }
                                    break;
                            }
                        }

                        if (!importAllOthers) {
                            builder = builder.importAllOthers().blankLine();
                        }

                        if (!importStaticAllOthers) {
                            builder = builder.importStaticAllOthers().blankLine();
                        }

                        // set lower limits in case type attribution is really messed up on the project
                        // and we can't effectively count star imports
                        builder.classCountToUseStarImport(Math.max(minimumFoldedImports, 5));
                        builder.nameCountToUseStarImport(Math.max(minimumFoldedStaticImports, 3));

                        return builder.build();
                    })
                    .orElse(IntelliJ.importLayout());
        }

        @EqualsAndHashCode
        static class Block {
            private final BlockType type;

            @Nullable
            private final String pattern;

            Block(BlockType type, List<J.Import> imports) {
                this.type = type;
                this.pattern = getPattern(imports);
            }

            static String getPattern(List<J.Import> imports) {
                String longestCommonPrefix = null;

                for (J.Import anImport : imports) {
                    String pkg = anImport.getPackageName();
                    longestCommonPrefix = longestCommonPrefix(pkg, longestCommonPrefix);
                    if (longestCommonPrefix.isEmpty()) {
                        return "all other imports";
                    }
                }

                if (longestCommonPrefix == null) {
                    return "all other imports";
                } else if (longestCommonPrefix.startsWith("java.")) {
                    return "java.*";
                } else if (longestCommonPrefix.startsWith("javax.")) {
                    return "javax.*";
                } else if (longestCommonPrefix.chars().filter(c -> c == '.').count() > 1) {
                    return longestCommonPrefix + "*";
                } else {
                    return "all other imports";
                }
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
            Set<ImportLayoutStatistics.Block> blocks = new LinkedHashSet<>();

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
                    blockStart = i;
                }

                if (anImport.getQualid().getSimpleName().equals("*")) {
                    if (anImport.isStatic()) {
                        int count = 0;
                        for (JavaType type : cu.getTypesInUse()) {
                            if (type instanceof JavaType.Variable) {
                                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(((JavaType.Variable) type).getType());
                                if (fq != null && anImport.getTypeName().equals(fq.getFullyQualifiedName())) {
                                    count++;
                                }
                            }
                        }

                        importLayoutStatistics.minimumFoldedStaticImports = Math.min(
                                importLayoutStatistics.minimumFoldedStaticImports,
                                count
                        );
                    } else {
                        Set<String> fqns = new HashSet<>();
                        for (JavaType type : cu.getTypesInUse()) {
                            if (type instanceof JavaType.FullyQualified) {
                                JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
                                if (anImport.getPackageName().equals(fq.getPackageName())) {
                                    // don't count directly, as JavaType.Parameterized can
                                    // CONTAIN a FullyQualified that matches a raw FullyQualified
                                    fqns.add(fq.getFullyQualifiedName());
                                }
                            }
                        }

                        importLayoutStatistics.minimumFoldedImports = Math.min(
                                importLayoutStatistics.minimumFoldedImports,
                                fqns.size()
                        );
                    }
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

            importLayoutStatistics.blocksPerSourceFile.add(new ArrayList<>(blocks));

            return cu;
        }
    }

    private static class SpacesStatistics {
        int beforeIf = 1;
        int beforeMethodCall = 0;
        int beforeMethodDeclaration = 0;
        int beforeFor = 1;
        int beforeWhile = 1;
        int beforeSwitch = 1;
        int beforeTry = 1;
        int beforeCatch = 1;
        int beforeSynchronized = 1;

        public SpacesStyle getSpacesStyle() {
            SpacesStyle spaces = IntelliJ.spaces();
            return spaces
                    .withBeforeParentheses(
                            new SpacesStyle.BeforeParentheses(
                                    beforeMethodDeclaration > 0,
                                    beforeMethodCall > 0,
                                    beforeIf > 0,
                                    beforeFor > 0 || beforeWhile > 0,
                                    beforeWhile > 0 || beforeFor > 0,
                                    beforeSwitch > 0,
                                    beforeTry > 0 || beforeCatch > 0,
                                    beforeTry > 0 || beforeCatch > 0,
                                    beforeSynchronized > 0,
                                    false
                            )
                    );
        }
    }

    private static class FindSpacesStyle extends JavaIsoVisitor<SpacesStatistics> {
        @Override
        public J.Try.Catch visitCatch(J.Try.Catch _catch, SpacesStatistics stats) {
            stats.beforeCatch += hasSpace(_catch.getParameter().getPrefix());
            return super.visitCatch(_catch, stats);
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, SpacesStatistics stats) {
            stats.beforeWhile += hasSpace(doWhileLoop.getWhileCondition().getPrefix());
            return super.visitDoWhileLoop(doWhileLoop, stats);
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, SpacesStatistics stats) {
            stats.beforeFor += hasSpace(forLoop.getControl().getPrefix());
            return super.visitForEachLoop(forLoop, stats);
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, SpacesStatistics stats) {
            stats.beforeFor += hasSpace(forLoop.getControl().getPrefix());
            return super.visitForLoop(forLoop, stats);
        }

        @Override
        public J.If visitIf(J.If iff, SpacesStatistics stats) {
            stats.beforeIf += hasSpace(iff.getIfCondition().getPrefix());
            return super.visitIf(iff, stats);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, SpacesStatistics stats) {
            stats.beforeMethodDeclaration += hasSpace(method.getPadding().getParameters().getBefore());
            return super.visitMethodDeclaration(method, stats);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, SpacesStatistics stats) {
            stats.beforeMethodCall += hasSpace(method.getPadding().getArguments().getBefore());
            return super.visitMethodInvocation(method, stats);
        }

        @Override
        public J.Switch visitSwitch(J.Switch _switch, SpacesStatistics stats) {
            stats.beforeSwitch += hasSpace(_switch.getSelector().getPrefix());
            return super.visitSwitch(_switch, stats);
        }

        @Override
        public J.Synchronized visitSynchronized(J.Synchronized _sync, SpacesStatistics stats) {
            stats.beforeSynchronized += hasSpace(_sync.getLock().getPrefix());
            return super.visitSynchronized(_sync, stats);
        }

        @Override
        public J.Try visitTry(J.Try _try, SpacesStatistics stats) {
            if(_try.getPadding().getResources() != null) {
                stats.beforeTry += hasSpace(_try.getPadding().getResources().getBefore());
            }
            return super.visitTry(_try, stats);
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, SpacesStatistics stats) {
            stats.beforeWhile += hasSpace(whileLoop.getCondition().getPrefix());
            return super.visitWhileLoop(whileLoop, stats);
        }

        private int hasSpace(Space space) {
            return space.getWhitespace().contains(" ") ? 1 : -1;
        }
    }
}
