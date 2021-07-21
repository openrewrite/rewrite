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
import org.openrewrite.internal.StringUtils;
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

        importLayoutStatistics.mapBlockPatterns(cus);
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
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            boolean useTabs = !isIndentedWithSpaces();
            boolean useCRLF = !isIndentedWithLFNewLines();

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
                    useTabs ? 1 : indent,
                    continuationIndent,
                    false,
                    useCRLF
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
            char[] chars = prefix.toCharArray();

            int indent = 0;
            // Note: new lines in multiline comments will not be counted.
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '\n' || c == '\r') {
                    if (c == '\n') {
                        if (i == 0 || chars[i - 1] != '\r') {
                            stats.linesWithLFNewLines++;
                        } else {
                            stats.linesWithCRLFNewLines++;
                        }
                    }

                    indent = 0;
                    continue;
                }
                if (Character.isWhitespace(c)) {
                    indent++;
                }
            }

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            if (prefix.chars()
                    .filter(c -> {
                        takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                        return takeWhile.get();
                    })
                    .count() > 0) {
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
        Map<String, String> pkgToBlockPattern = new LinkedHashMap<>();
        int staticAtTopCount = 0;
        int staticAtBotCount = 0;
        int minimumFoldedImports = Integer.MAX_VALUE;
        int minimumFoldedStaticImports = Integer.MAX_VALUE;

        public boolean isStaticImportsAtBot() {
            return staticAtBotCount >= staticAtTopCount;
        }

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
                        boolean insertAllOthers = false;
                        boolean importStaticAllOthers = true;
                        boolean addJavaOrJavax = true;
                        boolean containsJava = false;
                        boolean containsJavax = false;

                        List<Integer> countOfBlocksInGroup = new ArrayList<>(Collections.nCopies(longestBlocks.size(), 0));
                        int pos = 0;
                        int max = Integer.MIN_VALUE;
                        int insertAllOtherAtIndex = 0;
                        // The longest sequence of non-static imports without blank lines is converted
                        // into an 'all others' block.
                        for (int i = 0; i < longestBlocks.size(); i++) {
                            Block block = longestBlocks.get(i);
                            if (!containsJava && block.pattern.equals("java.*")) {
                                containsJava = true;
                            }

                            if (!containsJavax && block.pattern.equals("javax.*")) {
                                containsJavax = true;
                            }

                            if (BlockType.Import.equals(block.type)) {
                                countOfBlocksInGroup.set(pos, countOfBlocksInGroup.get(pos) + 1);
                            }

                            if (max < countOfBlocksInGroup.get(pos)) {
                                max = countOfBlocksInGroup.get(pos);
                                insertAllOtherAtIndex = pos;
                                insertAllOthers = true;
                            }

                            if (i + 1 < longestBlocks.size() - 1 && block.addBlankLine) {
                                pos = i + 1;
                            }
                        }

                        for (Block block : longestBlocks) {
                            if (!isStaticImportsAtBot()) {
                                if (importStaticAllOthers) {
                                    builder = builder.importStaticAllOthers();
                                    importStaticAllOthers = false;
                                }

                                if (BlockType.ImportStatic.equals(block.type)) {
                                    builder = builder.blankLine().staticImportPackage(block.pattern);
                                }
                            }
                        }

                        boolean addNewLine = !isStaticImportsAtBot();
                        if (!insertAllOthers) {
                            if (addNewLine) {
                                builder = builder.blankLine();
                            }

                            builder = builder.importAllOthers();
                            if (!containsJava && !containsJavax) {
                                builder = builder.blankLine().importPackage("javax.*");
                                builder = builder.blankLine().importPackage("java.*");
                            }
                            addNewLine = true;
                        }

                        for (int i = 0; i < longestBlocks.size(); i++) {
                            // Insert the all others block.
                            if (insertAllOthers) {
                                if (i == insertAllOtherAtIndex) {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                    }
                                    builder = builder.importAllOthers();
                                    if (!containsJava && !containsJavax) {
                                        builder = builder.blankLine().importPackage("javax.*");
                                        builder = builder.blankLine().importPackage("java.*");
                                    }
                                    builder = builder.blankLine();

                                    continue;
                                } else if (i > insertAllOtherAtIndex) {
                                    if (countOfBlocksInGroup.get(i) == 0) {
                                        continue;
                                    } else {
                                        insertAllOthers = false;
                                    }
                                }
                            }

                            Block block = longestBlocks.get(i);
                            if (BlockType.Import.equals(block.type)) {
                                if (addJavaOrJavax && block.pattern.equals("java.*")) {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                        addNewLine = false;
                                    }

                                    if (!((i - 1 >= 0 &&
                                            longestBlocks.get(i - 1).pattern.equals("javax.*")) ||
                                            (i + 1 < longestBlocks.size() - 1 &&
                                                    longestBlocks.get(i + 1).pattern.equals("javax.*")))) {
                                        builder = builder.importPackage("javax.*");
                                        builder = builder.blankLine().importPackage(block.pattern);
                                        addJavaOrJavax = false;
                                    } else {
                                        builder = builder.importPackage(block.pattern);
                                    }

                                    if (block.addBlankLine) {
                                        builder = builder.blankLine();
                                    }
                                } else if (addJavaOrJavax && block.pattern.equals("javax.*")) {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                        addNewLine = false;
                                    }

                                    if (!((i - 1 >= 0 &&
                                            longestBlocks.get(i - 1).pattern.equals("java.*")) ||
                                            (i + 1 < longestBlocks.size() - 1 &&
                                                    longestBlocks.get(i + 1).pattern.equals("java.*")))) {
                                        builder = builder.importPackage(block.pattern);
                                        builder = builder.blankLine().importPackage("java.*");
                                        addJavaOrJavax = false;
                                    } else {
                                        builder = builder.importPackage(block.pattern);
                                    }

                                    if (block.addBlankLine) {
                                        builder = builder.blankLine();
                                    }
                                } else {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                        addNewLine = false;
                                    }

                                    builder = builder.importPackage(block.pattern);

                                    if (block.addBlankLine) {
                                        builder = builder.blankLine();
                                    }
                                }
                            }
                        }

                        for (Block block : longestBlocks) {
                            if (isStaticImportsAtBot()) {
                                if (importStaticAllOthers) {
                                    if (BlockType.ImportStatic.equals(longestBlocks.get(0).type)) {
                                        builder = builder.blankLine();
                                    }

                                    builder = builder.importStaticAllOthers();
                                    importStaticAllOthers = false;
                                }

                                if (BlockType.ImportStatic.equals(block.type)) {
                                    builder = builder.blankLine().staticImportPackage(block.pattern);
                                }
                            }
                        }

                        if (longestBlocks.isEmpty()) {
                            builder.importAllOthers();
                            builder.blankLine();
                            builder.importStaticAllOthers();
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
            private final String pattern;
            private final boolean addBlankLine;

            Block(BlockType type, String pattern, boolean addBlankLine) {
                this.type = type;
                this.pattern = pattern;
                this.addBlankLine = addBlankLine;
            }
        }

        /**
         * Maps the imported packages to patterns used to create Blocks in the ImportLayout.
         * Patterns are generated early to prevent block patterns that are too specific.
         * Ex. org.openrewrite.* vs. org.openrewrite.java.test.*
         *
         * @param cus list of compilation units to create Block patterns from.
         */
        public void mapBlockPatterns(List<J.CompilationUnit> cus) {
            Set<String> importedPackages = new TreeSet<>();
            for (J.CompilationUnit cu : cus) {
                for (J.Import anImport : cu.getImports()) {
                    importedPackages.add(anImport.getPackageName() + ".");
                }
            }

            String longestCommonPrefix = null;
            String prevLCP = null;
            List<String> prevPackages = new ArrayList<>();

            for (String pkg : importedPackages) {
                longestCommonPrefix = longestCommonPrefix(pkg, longestCommonPrefix);
                if (!prevPackages.isEmpty() && longestCommonPrefix.chars().filter(c -> c == '.').count() <= 1 && !StringUtils.isNullOrEmpty(prevLCP)) {
                    for (String prev : prevPackages) {
                        if (prevLCP.startsWith("java.")) {
                            prevLCP = "java.";
                        } else if (prevLCP.startsWith("javax.")) {
                            prevLCP = "javax.";
                        }
                        this.pkgToBlockPattern.put(prev, prevLCP + "*");
                    }
                    longestCommonPrefix = pkg;
                    prevPackages.clear();
                }

                prevPackages.add(pkg);
                prevLCP = longestCommonPrefix;
            }

            for (String prev : prevPackages) {
                this.pkgToBlockPattern.put(prev, prevLCP + "*");
            }
        }

        private String longestCommonPrefix(String pkg, @Nullable String lcp) {
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

    private static class FindImportLayout extends JavaIsoVisitor<ImportLayoutStatistics> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ImportLayoutStatistics importLayoutStatistics) {
            Set<ImportLayoutStatistics.Block> blocks = new LinkedHashSet<>();

            importLayoutStatistics.staticAtBotCount += (cu.getImports().size() > 0 &&
                    cu.getImports().get(cu.getImports().size() - 1).isStatic()) ? 1 : 0;
            importLayoutStatistics.staticAtTopCount += (cu.getImports().size() > 0 &&
                    cu.getImports().get(0).isStatic()) ? 1 : 0;

            boolean staticBlock = false;
            int blockStart = 0;
            int i = 0;
            String previousPkg = "";
            int previousPkgCount = 0;
            Map<ImportLayoutStatistics.Block, Integer> referenceCount = new HashMap<>();
            for (J.Import anImport : cu.getImports()) {
                previousPkgCount += previousPkg != null && previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + ".")) ? 1 : 0;
                if (anImport.getPrefix().getWhitespace().contains("\n\n") || anImport.getPrefix().getWhitespace().contains("\r\n\r\n") ||
                        i > 0 && previousPkg != null && importLayoutStatistics.pkgToBlockPattern.containsKey(anImport.getPackageName() + ".") &&
                                !previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + "."))) {
                    if (i - blockStart > 0) {
                        assert previousPkg != null;
                        ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                                staticBlock ?
                                        ImportLayoutStatistics.BlockType.ImportStatic :
                                        ImportLayoutStatistics.BlockType.Import,
                                previousPkg,
                                anImport.getPrefix().getWhitespace().contains("\n\n") || anImport.getPrefix().getWhitespace().contains("\r\n\r\n"));

                        if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                            blocks.remove(block);
                        }
                        blocks.add(block);
                        referenceCount.put(block, previousPkgCount + 1);
                        previousPkgCount = 0;
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
                previousPkg = importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + ".");
            }

            if (i - blockStart > 0) {
                ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                        staticBlock ?
                                ImportLayoutStatistics.BlockType.ImportStatic :
                                ImportLayoutStatistics.BlockType.Import,
                        previousPkg,
                        false);

                if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                    blocks.remove(block);
                }
                blocks.add(block);
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
