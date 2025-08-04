/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.*;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.kotlin.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static class Detector {

        private final IndentStatistics indentStatistics = new IndentStatistics();
        private final SpacesStatistics spacesStatistics = new SpacesStatistics();
        private final WrappingAndBracesStatistics wrappingAndBracesStatistics = new WrappingAndBracesStatistics();
        private final GeneralFormatStatistics generalFormatStatistics = new GeneralFormatStatistics();
        private final TrailingCommaStatistics trailingCommaStatistics = new TrailingCommaStatistics();

        private final FindImportLayout findImportLayout = new FindImportLayout();
        private final FindIndentJavaVisitor findIndent = new FindIndentJavaVisitor();
        private final FindSpacesStyle findSpaces = new FindSpacesStyle();
        private final FindWrappingAndBracesStyle findWrappingAndBraces = new FindWrappingAndBracesStyle();
        private final FindLineFormatJavaVisitor findLineFormat = new FindLineFormatJavaVisitor();
        private final FindTrailingCommaVisitor findTrailingComma = new FindTrailingCommaVisitor();

        public void sample(SourceFile cu) {
            if (cu instanceof K.CompilationUnit) {
                findImportLayout.visitNonNull(cu, 0);
                findIndent.visitNonNull(cu, indentStatistics);
                findSpaces.visitNonNull(cu, spacesStatistics);
                findWrappingAndBraces.visitNonNull(cu, wrappingAndBracesStatistics);
                findLineFormat.visitNonNull(cu, generalFormatStatistics);
                findTrailingComma.visitNonNull(cu, trailingCommaStatistics);
            }
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    indentStatistics.getTabsAndIndentsStyle(),
                    findImportLayout.getImportLayoutStyle(),
                    spacesStatistics.getSpacesStyle(),
                    wrappingAndBracesStatistics.getWrappingAndBracesStyle(),
                    generalFormatStatistics.getFormatStyle(),
                    trailingCommaStatistics.getOtherStyle()
            ));
        }
    }

    private static class GeneralFormatStatistics {
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public GeneralFormatStyle getFormatStyle() {
            boolean useCRLF = !isIndentedWithLFNewLines();

            return new GeneralFormatStyle(useCRLF);
        }
    }

    @Data
    private static class IndentStatistic {
        @Value
        private static class DepthCoordinate {
            int indentDepth;
            int continuationDepth;
        }

        // depth -> count of whitespace char (4 spaces, 2 tabs, etc) -> count of occurrences
        Map<DepthCoordinate, Map<Integer, Long>> depthToSpaceIndentFrequencies = new ConcurrentHashMap<>();

        public void record(int indentDepth, int continuationDepth, int charCount) {
            record(new DepthCoordinate(indentDepth, continuationDepth), charCount);
        }

        public void record(DepthCoordinate depth, int charCount) {
            if (charCount <= 0) {
                return;
            }
            depthToSpaceIndentFrequencies.compute(depth, (n, map) -> {
                if (map == null) {
                    map = new ConcurrentHashMap<>();
                }
                map.compute(charCount, (m, count) -> {
                    if (count == null) {
                        count = 0L;
                    }
                    return count + 1;
                });
                return map;
            });
        }


        /**
         * Use the provided common indentation to interpret this IndentStatistic's contents as continuation indents.
         * Continuation indents are assumed to be offset by some constant number of characters from what their depth
         * would normally indicate.
         */
        public int continuationIndent(int commonIndent) {
            Map<Integer, Long> map = depthToSpaceIndentFrequencies.entrySet().stream()
                    .flatMap(it -> {
                        int depth = it.getKey().getIndentDepth();
                        int continuationDepth = it.getKey().getContinuationDepth();
                        return it.getValue().entrySet()
                                .stream()
                                .map(charsToOccurrence -> new AbstractMap.SimpleEntry<>(
                                        (charsToOccurrence.getKey() - (depth * commonIndent)) / continuationDepth,
                                        charsToOccurrence.getValue()));
                    })
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, Long::sum));

            return map.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(commonIndent * 2);
        }
    }

    private static class IndentStatistics {
        private final IndentStatistic spaceIndentFrequencies = new IndentStatistic();
        private final IndentStatistic spaceContinuationIndentFrequencies = new IndentStatistic();
        private final IndentStatistic tabIndentFrequencies = new IndentStatistic();
        private final IndentStatistic tabContinuationIndentFrequencies = new IndentStatistic();
        private final IndentStatistic deltaSpaceIndentFrequencies = new IndentStatistic();
        private long accumulateDepthCount = 0;
        private int multilineAlignedToFirstArgument = 0;
        private int multilineNotAlignedToFirstArgument = 0;

        @Getter
        private int depth = 0;

        @Getter
        private int continuationDepth = 1;

        public void incrementDepth() {
            depth++;
        }

        public void decrementDepth() {
            depth--;
        }

        // Leave these unused methods in case we want to extend autodetection to method invocation argument lists
        @SuppressWarnings("unused")
        public void incrementContinuationDepth() {
            continuationDepth++;
        }

        @SuppressWarnings("unused")
        public void decrementContinuationDepth() {
            continuationDepth--;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            /*
             * For each line, if the code follows an indentation style exactly,
             * Assume :
             *      nw = space count in prefix
             *      nt = tabs count in prefix
             *      d = block depth
             *      X = tabSize, which means space count per tab, unknown to be solved here.
             * So theoretically the formula is:
             *      d = nt + (nw / X)                                                                       (1)
             *      X = nw / (d - nt)                                                                       (2)
             * Because this is a linear equation, it also applies to the sum of multiple values, that is:
             *          d1 + d2 + .. + dn = (nt1 + nt2 + .. + ntn) + (nw1 +nw2 + ... + nwn) / X             (3)
             * So let's define:
             *      D = d1 + d2 + .. + dn, which is the total count of depth.
             *      NT = nt1 + nt2 + .. + ntn, which is the total count of tabs.
             *      NW = nw1 +nw2 + ... + nwn, which is the total count of spaces.
             * So
             *      D = NT + (NW / X)                                                                       (4)
             *      X = NW / (D - NT)                                                                       (5)
             * the more data (more lines of code), the more accuracy.
             *
             * (1) How to determine the `useTabs` boolean value?
             * From formula #4, D is composed of two parts, the Tabs part (NT) and the spaces part (NW / X).
             * so `useTabs` should be determined by which part is bigger (> 50%).
             * define:
             *      PT = (NT / D), which means the percentage of tabs.
             * So
             *      useTabs = PT > 0.5
             */
            if (this.accumulateDepthCount == 0) {
                return IntelliJ.tabsAndIndents();
            }

            long d = this.accumulateDepthCount;                     // D in above comments
            long nt = getTotalCharCount(tabIndentFrequencies);      // NT in above comments
            double pt = nt / (double) d;                            // PT in above comments
            boolean useTabs = pt >= 0.5;

            // Calculate tabSize based on the frequency, pick up the biggest frequency group.
            // Using frequency are less susceptible to outliers than means.
            int moreFrequentTabSize = getBiggestGroupOfTabSize(deltaSpaceIndentFrequencies);
            int tabSize = (moreFrequentTabSize == 0) ? 4 : moreFrequentTabSize;

            IndentStatistic continuationFrequencies = useTabs ? tabContinuationIndentFrequencies : spaceContinuationIndentFrequencies;
            int continuationIndent = continuationFrequencies.continuationIndent(useTabs ? 1 : tabSize) * (useTabs ? tabSize : 1);
            return new TabsAndIndentsStyle(
                    useTabs,
                    tabSize,
                    tabSize,
                    continuationIndent,
                    false,
                    new TabsAndIndentsStyle.FunctionDeclarationParameters(
                            multilineAlignedToFirstArgument >= multilineNotAlignedToFirstArgument)
            );
        }
    }

    private static class TrailingCommaStatistics {
        private long usedTrailingCommaCount = 0;
        private long unusedTrailingCommaCount = 0;

        public OtherStyle getOtherStyle() {
            boolean useTrailingComma = usedTrailingCommaCount > unusedTrailingCommaCount;
            return new OtherStyle(useTrailingComma);
        }
    }

    private static long getTotalCharCount(IndentStatistic indentStatistic) {
        return indentStatistic.depthToSpaceIndentFrequencies.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().entrySet().stream())
                .mapToLong(entry -> entry.getKey() * entry.getValue())
                .sum();
    }

    private static int getBiggestGroupOfTabSize(IndentStatistic deltaSpaces) {
        Map<Integer, Integer> tabSizeToFrequencyMap = deltaSpaces.depthToSpaceIndentFrequencies.entrySet().stream()
                .filter(entry -> entry.getKey().indentDepth != 0)
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(spaceCountToFrequency -> new AbstractMap.SimpleEntry<>(
                                (int) Math.round(spaceCountToFrequency.getKey() / (double) entry.getKey().indentDepth),
                                spaceCountToFrequency.getValue().intValue())))
                .collect(groupingBy(Map.Entry::getKey, summingInt(Map.Entry::getValue)));

        return tabSizeToFrequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private static class FindLineFormatJavaVisitor extends KotlinIsoVisitor<GeneralFormatStatistics> {

        @Override
        public @Nullable J visit(@Nullable Tree tree, GeneralFormatStatistics generalFormatStatistics) {
            try {
                super.visit(tree, generalFormatStatistics);
            } catch (Exception e) {
                // Suppress errors. A malformed element should not fail parsing overall.
            }
            return (J) tree;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, GeneralFormatStatistics stats) {
            String prefix = space.getWhitespace();

            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);
                if (c == '\n') {
                    if (i == 0 || prefix.charAt(i - 1) != '\r') {
                        stats.linesWithLFNewLines++;
                    } else {
                        stats.linesWithCRLFNewLines++;
                    }
                }
            }
            return space;
        }
    }

    private static class FindIndentJavaVisitor extends KotlinIsoVisitor<IndentStatistics> {

        @Override
        public @Nullable J visit(@Nullable Tree tree, IndentStatistics indentStatistics) {
            try {
                super.visit(tree, indentStatistics);
            } catch (Exception e) {
                // Suppress errors. A malformed element should not fail parsing overall.
            }
            return (J) tree;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, IndentStatistics stats) {
            // Only visit things we're interested in getting indentation from
            visitStatement(cd, stats);
            visit(cd.getBody(), stats);
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, IndentStatistics stats) {
            if (method.getParameters().size() > 1) {
                int alignTo;
                if (method.getParameters().get(0).getPrefix().getLastWhitespace().contains("\n")) {
                    // Compare to the prefix of the first arg.
                    alignTo = method.getParameters().get(0).getPrefix().getLastWhitespace().length() - 1;
                } else {
                    String source = method.print(getCursor().getParentOrThrow());
                    alignTo = source.indexOf(method.getParameters().get(0).print(getCursor())) - 1;
                }
                List<Statement> parameters = method.getParameters();
                for (int i = 1; i < parameters.size(); i++) {
                    if (parameters.get(i).getPrefix().getLastWhitespace().contains("\n")) {
                        if (alignTo == parameters.get(i).getPrefix().getLastWhitespace().length() - 1) {
                            stats.multilineAlignedToFirstArgument++;
                        } else {
                            stats.multilineNotAlignedToFirstArgument++;
                            countIndents(parameters.get(i).getPrefix().getWhitespace(), true, stats);
                        }
                    }
                }
            }
            // Visit only those parts of the declaration we're interested in getting indentation info from
            visitStatement(method, stats);
            ListUtils.map(method.getLeadingAnnotations(), a -> visitAndCast(a, stats));
            visitContainer(method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, stats);
            visit(method.getBody(), stats);
            return method;
        }

        @Override
        public J.Block visitBlock(J.Block block, IndentStatistics stats) {
            stats.incrementDepth();
            for (Statement s : block.getStatements()) {
                if (s instanceof Expression) {
                    // Some statement types, like method invocations, are also expressions
                    // If an expression appears in a context where statements are expected, its indent is not a continuation
                    Set<Expression> statementExpressions = getCursor()
                            .getMessage("STATEMENT_EXPRESSION", newSetFromMap(
                                    new IdentityHashMap<>(block.getStatements().size())));
                    statementExpressions.add((Expression) s);
                    getCursor().putMessage("STATEMENT_EXPRESSION", statementExpressions);
                }
                visit(s, stats);
            }
            stats.decrementDepth();
            return block;
        }

        @Override
        public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, IndentStatistics indentStatistics) {
            return annotatedType;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, IndentStatistics stats) {
            visitStatement(vd, stats);
            ListUtils.map(vd.getPadding().getVariables(), t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, stats));
            return vd;
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray na, IndentStatistics stats) {
            visitExpression(na, stats);
            return na;
        }

        @Override
        public Statement visitStatement(Statement statement, IndentStatistics stats) {
            boolean isInParentheses = getCursor().dropParentUntil(
                    p -> p instanceof J.Block ||
                         p instanceof JContainer ||
                         p instanceof SourceFile).getValue() instanceof JContainer;
            if (isInParentheses) {
                // ignore statements in parentheses.
                return statement;
            }

            countIndents(statement.getPrefix().getWhitespace(), false, stats);
            return statement;
        }

        @Override
        public Expression visitExpression(Expression expression, IndentStatistics stats) {
            super.visitExpression(expression, stats);
            Set<Expression> statementExpressions = getCursor().getNearestMessage("STATEMENT_EXPRESSION", emptySet());
            if (statementExpressions.contains(expression)) {
                return expression;
            }
            // (newline-separated) annotations on some common target are not continuations
            // (newline-separated) annotations on some common target are not continuations
            boolean isContinuation = !(expression instanceof J.Annotation && !(
                    // ...but annotations which are *arguments* to other annotations can be continuations
                    getCursor().getParentTreeCursor().getValue() instanceof J.Annotation ||
                    getCursor().getParentTreeCursor().getValue() instanceof J.NewArray
            ));
            countIndents(expression.getPrefix().getWhitespace(), isContinuation, stats);

            return expression;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fa, IndentStatistics stats) {
            visit(fa.getTarget(), stats);
            countIndents(fa.getPadding().getName().getBefore().getWhitespace(), true, stats);
            return fa;
        }

        @SuppressWarnings("CommentedOutCode")
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, IndentStatistics stats) {
            Set<Expression> statementExpressions = getCursor().getNearestMessage("STATEMENT_EXPRESSION", emptySet());
            if (statementExpressions.contains(m)) {
                visitStatement(m, stats);
            } else {
                visitExpression(m, stats);
            }
            if (m.getPadding().getSelect() != null) {
                countIndents(m.getPadding().getSelect().getAfter().getWhitespace(), true, stats);
                visit(m.getSelect(), stats);
            }
            visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, stats);

            /*
             * The treatment of continuations in method argument lists is particularly head-hurting.
             * Some IDEs will do things like indent arguments differently if the current invocation is another invocation's select
             * For now avoid this source of confusion and inconsistency.
             */
//            stats.incrementContinuationDepth();
//            visitContainer(m.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, stats);
//            stats.decrementContinuationDepth();
            return m;
        }

        @Override
        public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, IndentStatistics indentStatistics) {
            return super.visitRightPadded(right, loc, indentStatistics);
        }

        private void countIndents(String space, boolean isContinuation, IndentStatistics stats) {
            int ni = space.lastIndexOf('\n');
            int depth = stats.getDepth();
            if (ni >= 0 && depth > 0) {
                int spaceIndent = 0;
                int tabIndent = 0;
                boolean mixed = false;
                for (int i = ni; i < space.length(); i++) {
                    if (space.charAt(i) == ' ') {
                        if (tabIndent > 0) {
                            mixed = true;
                        }
                        spaceIndent++;
                    } else if (space.charAt(i) == '\t') {
                        if (spaceIndent > 0) {
                            mixed = true;
                        }
                        tabIndent++;
                    }
                }
                if (spaceIndent > 0 || tabIndent > 0) {
                    if (!isContinuation) {
                        stats.spaceIndentFrequencies.record(depth, 0, spaceIndent);
                        stats.tabIndentFrequencies.record(depth, 0, tabIndent);
                        stats.deltaSpaceIndentFrequencies.record(depth - tabIndent, 0, spaceIndent);
                        stats.accumulateDepthCount += depth;
                    } else if (!mixed) {
                        stats.spaceContinuationIndentFrequencies.record(depth, stats.getContinuationDepth(), spaceIndent);
                        stats.tabContinuationIndentFrequencies.record(depth, stats.getContinuationDepth(), tabIndent);
                    }
                }
            }
        }
    }

    private static class FindTrailingCommaVisitor extends KotlinIsoVisitor<TrailingCommaStatistics> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, TrailingCommaStatistics statistics) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, statistics);
            sample(m.getPadding().getParameters(), statistics);
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, TrailingCommaStatistics statistics) {
            J.MethodInvocation m = super.visitMethodInvocation(method, statistics);
            sample(m.getPadding().getArguments(), statistics);
            return m;
        }

        private <T extends J> void sample(JContainer<T> container, TrailingCommaStatistics statistics) {
            List<JRightPadded<T>> rps = container.getPadding().getElements();

            if (!rps.isEmpty()) {
                JRightPadded<T> last = rps.get(rps.size() - 1);

                Markers markers = last.getMarkers();
                Optional<TrailingComma> maybeTrailingComma = markers.findFirst(TrailingComma.class);
                if (maybeTrailingComma.isPresent()) {
                    statistics.usedTrailingCommaCount++;
                } else {
                    statistics.unusedTrailingCommaCount++;
                }
            }
        }
    }

    private static class ImportLayoutStatistics {
        List<List<Block>> blocksPerSourceFile = new ArrayList<>();
        Map<String, String> pkgToBlockPattern = new LinkedHashMap<>();
        int javaBeforeJavaxCount = 0;
        int javaxBeforeJavaCount = 0;
        int minimumFoldedImports = Integer.MAX_VALUE;
        int minimumFoldedStaticImports = Integer.MAX_VALUE;

        public boolean isJavaxBeforeJava() {
            return javaxBeforeJavaCount >= javaBeforeJavaxCount;
        }

        enum BlockType {
            Import
        }

        @SuppressWarnings("DuplicatedCode")
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
                        boolean containsJava = false;
                        boolean containsJavax = false;

                        int insertAllOtherAtIndex = 0;
                        int nonStaticMaxCount = Integer.MIN_VALUE;
                        int nonStaticCountPos = 0;
                        int nonStaticPos = 0;

                        List<Block> nonStaticBlocks = new ArrayList<>(); // Isolate static imports to add at top or bottom of layout.
                        List<Integer> countOfBlocksInNonStaticGroups = new ArrayList<>();

                        for (Block block : longestBlocks) {
                            if (!containsJava && "java.*".equals(block.pattern)) {
                                containsJava = true;
                            }

                            if (!containsJavax && "javax.*".equals(block.pattern)) {
                                containsJavax = true;
                            }

                            nonStaticBlocks.add(block);
                            countOfBlocksInNonStaticGroups.add(0);
                            countOfBlocksInNonStaticGroups.set(nonStaticCountPos,
                                    countOfBlocksInNonStaticGroups.get(nonStaticCountPos) + 1);
                            if (nonStaticMaxCount < countOfBlocksInNonStaticGroups.get(nonStaticCountPos)) {
                                nonStaticMaxCount = countOfBlocksInNonStaticGroups.get(nonStaticCountPos);
                                insertAllOtherAtIndex = nonStaticCountPos;
                                insertAllOthers = true;
                            }

                            if (block.addBlankLine) {
                                nonStaticCountPos = nonStaticPos + 1;
                            }
                            nonStaticPos++;
                        }

                        // Add static imports at the top if it's the standard.
                        boolean addNewLine = false;

                        // There are no non-static imports, add a block of all other import.
                        if (!insertAllOthers) {

                            builder = builder.importAllOthers();
                            // Add java/javax if they're missing from the block that is being used as a template.
                            if (!containsJava && !containsJavax) {
                                builder = builder.blankLine();
                                if (isJavaxBeforeJava()) {
                                    builder = builder.importPackage("javax.*");
                                    builder = builder.importPackage("java.*");
                                } else {
                                    builder = builder.importPackage("java.*");
                                    builder = builder.importPackage("javax.*");
                                }
                            }
                            addNewLine = true;
                        }

                        boolean addJavaOrJavax = true; // Used to normalize the pos of java and javax imports.
                        for (int i = 0; i < nonStaticBlocks.size(); i++) {
                            if (insertAllOthers) {
                                // Insert the all others block.
                                if (i == insertAllOtherAtIndex) {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                        addNewLine = false;
                                    }
                                    builder = builder.importAllOthers();
                                    // Add java/javax if they're missing from the block that is being used as a template.
                                    if (!containsJava && !containsJavax) {
                                        builder = builder.blankLine();
                                        if (isJavaxBeforeJava()) {
                                            builder = builder.importPackage("javax.*");
                                            builder = builder.importPackage("java.*");
                                        } else {
                                            builder = builder.importPackage("java.*");
                                            builder = builder.importPackage("javax.*");
                                        }
                                    }
                                    continue;
                                } else if (i > insertAllOtherAtIndex) {
                                    if (countOfBlocksInNonStaticGroups.get(i) == 0) {
                                        continue;
                                    } else {
                                        insertAllOthers = false;
                                        addNewLine = true;
                                    }
                                }
                            }

                            Block block = nonStaticBlocks.get(i);
                            if (addJavaOrJavax && "java.*".equals(block.pattern)) {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                if (!(i - 1 >= 0 && "javax.*".equals(nonStaticBlocks.get(i - 1).pattern) ||
                                      i + 1 < nonStaticBlocks.size() && "javax.*".equals(nonStaticBlocks.get(i + 1).pattern))) {
                                    if (isJavaxBeforeJava()) {
                                        builder = builder.importPackage("javax.*");
                                        builder = builder.importPackage("java.*");
                                    } else {
                                        builder = builder.importPackage("java.*");
                                        builder = builder.importPackage("javax.*");
                                    }
                                    addNewLine = true;
                                    addJavaOrJavax = false;
                                } else {
                                    builder = builder.importPackage(block.pattern);
                                }
                            } else if (addJavaOrJavax && "javax.*".equals(block.pattern)) {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                if (!(i - 1 >= 0 && "java.*".equals(nonStaticBlocks.get(i - 1).pattern) ||
                                      i + 1 < nonStaticBlocks.size() - 1 && "java.*".equals(nonStaticBlocks.get(i + 1).pattern))) {
                                    if (isJavaxBeforeJava()) {
                                        builder = builder.importPackage("javax.*");
                                        builder = builder.importPackage("java.*");
                                    } else {
                                        builder = builder.importPackage("java.*");
                                        builder = builder.importPackage("javax.*");
                                    }
                                    addNewLine = true;
                                    addJavaOrJavax = false;
                                } else {
                                    builder = builder.importPackage(block.pattern);
                                }
                            } else {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                builder = builder.importPackage(block.pattern);
                            }
                            if (block.addBlankLine && i != nonStaticBlocks.size() - 1) {
                                builder = builder.blankLine();
                            }
                        }

                        // Add statics at bottom.
                        builder = builder.blankLine();

                        // There are no static imports, add an all other import block.
                        builder = builder.importAllOthers();


                        if (longestBlocks.isEmpty()) {
                            builder.importAllOthers();
                            builder.blankLine();
                            if (isJavaxBeforeJava()) {
                                builder = builder.importPackage("javax.*");
                                builder = builder.importPackage("java.*");
                            } else {
                                builder = builder.importPackage("java.*");
                                builder = builder.importPackage("javax.*");
                            }
                            builder.blankLine();
                            builder.importAllOthers();
                        }

                        // set lower limits in case type attribution is really messed up on the project
                        // and we can't effectively count star imports
                        builder.topLevelSymbolsToUseStarImport(Math.max(minimumFoldedImports, 5));
                        builder.javaStaticsAndEnumsToUseStarImport(Math.max(minimumFoldedStaticImports, 3));

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
         */
        public void mapBlockPatterns(NavigableSet<String> importedPackages) {
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

            int i = 0;
            for (; i < pkg.length() && i < lcp.length(); i++) {
                if (pkg.charAt(i) != lcp.charAt(i)) {
                    break;
                }
            }
            return lcp.substring(0, i);
        }
    }

    @Value
    private static class ImportAttributes {
        String packageName;
        String prefix;
        boolean isAlias;
    }

    private static class FindImportLayout extends KotlinIsoVisitor<Integer> {
        private final List<List<ImportAttributes>> importsBySourceFile = new ArrayList<>();
        private final NavigableSet<String> importedPackages = new TreeSet<>();
        private final ImportLayoutStatistics importLayoutStatistics = new ImportLayoutStatistics();

        private static final String TYPE_ALL_OTHERS = "allOther";
        private static final String TYPE_JAVA = "java";
        private static final String TYPE_JAVAX = "javax";
        private static final String TYPE_KOTLIN = "kotlin";
        private static final String TYPE_ALL_ALIASES = "allAliases";

        private static final Double AVE_DEFAULT_WEIGHT_ALL_OTHERS = 0.5;
        private static final Double AVE_DEFAULT_WEIGHT_JAVA = 0.4;
        private static final Double AVE_DEFAULT_WEIGHT_JAVAX = 0.3;
        private static final Double AVE_DEFAULT_WEIGHT_KOTLIN = 0.2;
        private static final Double AVE_DEFAULT_WEIGHT_ALL_ALIASES = 0.1;


        public ImportLayoutStyle getImportLayoutStyle() {
            return importsBySourceFile.stream()
                    .max(Comparator.comparing(List::size))
                    .map(longestImports -> {
                        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
                        builder.packageToFold("kotlinx.android.synthetic.*", true)
                                .packageToFold("io.ktor.*", true);

                        Map<String, List<Integer>> weightMap = new HashMap<>();

                        for (int i = 0; i < longestImports.size(); i++) {
                            ImportAttributes importAttributes = longestImports.get(i);
                            int weight = longestImports.size() - i;
                            if (importAttributes.isAlias()) {
                                weightMap.computeIfAbsent(TYPE_ALL_ALIASES, k -> new ArrayList<>()).add(weight);
                            } else if (importAttributes.getPackageName().startsWith("java.")) {
                                weightMap.computeIfAbsent(TYPE_JAVA, k -> new ArrayList<>()).add(weight);
                            } else if (importAttributes.getPackageName().startsWith("javax.")) {
                                weightMap.computeIfAbsent(TYPE_JAVAX, k -> new ArrayList<>()).add(weight);
                            } else if (importAttributes.getPackageName().startsWith("kotlin.")) {
                                weightMap.computeIfAbsent(TYPE_KOTLIN, k -> new ArrayList<>()).add(weight);
                            } else {
                                weightMap.computeIfAbsent(TYPE_ALL_OTHERS, k -> new ArrayList<>()).add(weight);
                            }
                        }

                        Map<String, Double> averageWeightMap = weightMap.entrySet().stream().collect(toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    List<Integer> weights = entry.getValue();
                                    int sum = 0;
                                    for (int number : weights) {
                                        sum += number;
                                    }
                                    return (double) sum / weights.size();
                                }
                        ));

                        averageWeightMap.computeIfAbsent(TYPE_ALL_OTHERS, w -> AVE_DEFAULT_WEIGHT_ALL_OTHERS);
                        averageWeightMap.computeIfAbsent(TYPE_JAVA, w -> AVE_DEFAULT_WEIGHT_JAVA);
                        averageWeightMap.computeIfAbsent(TYPE_JAVAX, w -> AVE_DEFAULT_WEIGHT_JAVAX);
                        averageWeightMap.computeIfAbsent(TYPE_KOTLIN, w -> AVE_DEFAULT_WEIGHT_KOTLIN);
                        averageWeightMap.computeIfAbsent(TYPE_ALL_ALIASES, w -> AVE_DEFAULT_WEIGHT_ALL_ALIASES);

                        List<String> sortedTypes = sortTypesByWeightDescending(averageWeightMap);

                        for (String type : sortedTypes) {
                            switch (type) {
                                case TYPE_ALL_OTHERS:
                                    builder.importAllOthers();
                                    break;
                                case TYPE_JAVA:
                                    builder.importPackage("java.*");
                                    break;
                                case TYPE_JAVAX:
                                    builder.importPackage("javax.*");
                                    break;
                                case TYPE_KOTLIN:
                                    builder.importPackage("kotlin.*");
                                    break;
                                case TYPE_ALL_ALIASES:
                                    builder.importAllAliases();
                                    break;
                            }
                        }

                        return builder.build();
                    })
                    .orElse(IntelliJ.importLayout());
        }

        public static List<String> sortTypesByWeightDescending(Map<String, Double> averageWeightMap) {
            return averageWeightMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(toList());
        }

        public ImportLayoutStatistics aggregate() {
            // initializes importLayoutStatistics.pkgToBlockPattern which is used in the loop that follows
            importLayoutStatistics.mapBlockPatterns(importedPackages);

            for (List<ImportAttributes> imports : importsBySourceFile) {
                Set<ImportLayoutStatistics.Block> blocks = new LinkedHashSet<>();
                int blockStart = 0;
                int i = 0;
                String previousPkg = "";
                int previousPkgCount = 1;
                int javaPos = Integer.MAX_VALUE;
                int javaxPos = Integer.MAX_VALUE;
                Map<ImportLayoutStatistics.Block, Integer> referenceCount = new HashMap<>();
                for (ImportAttributes anImport : imports) {
                    previousPkgCount += previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + ".")) ? 1 : 0;
                    boolean containsNewLine = anImport.getPrefix().contains("\n\n") || anImport.getPrefix().contains("\r\n\r\n");
                    if (containsNewLine ||
                        i > 0 && importLayoutStatistics.pkgToBlockPattern.containsKey(anImport.getPackageName() + ".") &&
                        !previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + "."))) {
                        if (i - blockStart > 0) {
                            ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                                    ImportLayoutStatistics.BlockType.Import,
                                    previousPkg,
                                    containsNewLine);

                            javaPos = "java.*".equals(block.pattern) && javaPos > blockStart ? blockStart : javaPos;
                            javaxPos = "javax.*".equals(block.pattern) && javaxPos > blockStart ? blockStart : javaxPos;

                            if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                                blocks.remove(block);
                            }
                            blocks.add(block);
                            referenceCount.put(block, previousPkgCount + 1);
                            previousPkgCount = 1;
                        }

                        blockStart = i;
                    }

                    i++;
                    previousPkg = importLayoutStatistics.pkgToBlockPattern.getOrDefault(anImport.getPackageName() + ".", "");
                }

                if (i - blockStart > 0) {
                    ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                            ImportLayoutStatistics.BlockType.Import,
                            previousPkg,
                            false);

                    if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                        blocks.remove(block);
                    }

                    javaPos = "java.*".equals(block.pattern) ? blockStart : javaPos;
                    javaxPos = "javax.*".equals(block.pattern) ? blockStart : javaxPos;
                    blocks.add(block);
                }

                if (javaPos != Integer.MAX_VALUE && javaxPos != Integer.MAX_VALUE) {
                    importLayoutStatistics.javaBeforeJavaxCount += javaPos < javaxPos ? 1 : 0;
                    importLayoutStatistics.javaxBeforeJavaCount += javaxPos < javaPos ? 1 : 0;
                }

                importLayoutStatistics.blocksPerSourceFile.add(new ArrayList<>(blocks));
            }

            return importLayoutStatistics;
        }

        @Override
        public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, Integer integer) {
            for (J.Import anImport : cu.getImports()) {
                if (anImport.getQualid().getTarget() instanceof J.Empty) {
                    // skip unqualified imports
                    continue;
                }
                importedPackages.add(anImport.getPackageName() + ".");

                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (anImport.isStatic()) {
                        int count = 0;
                        for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getType());
                            if (fq != null && anImport.getTypeName().equals(fq.getFullyQualifiedName())) {
                                count++;
                            }
                        }

                        importLayoutStatistics.minimumFoldedStaticImports = Math.min(
                                importLayoutStatistics.minimumFoldedStaticImports,
                                count
                        );
                    } else {
                        Set<String> fqns = new HashSet<>();
                        for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
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
            }

            importsBySourceFile.add(cu.getImports().stream()
                    // skip unqualified imports
                    .filter(i -> !(i.getQualid().getTarget() instanceof J.Empty))
                    .map(it -> new ImportAttributes(it.getPackageName(),
                            it.getPrefix().getWhitespace(),
                            it.getAlias() != null))
                    .collect(toList()));
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
        int beforeComma = 0;
        int afterComma = 1;
        int beforeColonInForEach = 1;
        int beforeForSemiColon = 0;
        int afterForSemiColon = 0;
        int afterTypeCast = 0;
        int withinMethodCallParentheses = 0;

        public SpacesStyle getSpacesStyle() {
            SpacesStyle spaces = IntelliJ.spaces();
            return spaces
                    .withBeforeParentheses(
                            new SpacesStyle.BeforeParentheses(
                                    beforeIf > 0,
                                    beforeFor > 0 || beforeWhile > 0,
                                    beforeWhile > 0 || beforeFor > 0,
                                    beforeTry > 0 || beforeCatch > 0,
                                    true
                            )
                    )
                    .withOther(new SpacesStyle.Other(
                            beforeComma > 0,
                            afterComma >= 1,
                            false, true, true, true, true, true, true, true
                    ));
        }
    }

    private static class FindSpacesStyle extends KotlinIsoVisitor<SpacesStatistics> {

        @Override
        public @Nullable J visit(@Nullable Tree tree, SpacesStatistics spacesStatistics) {
            try {
                super.visit(tree, spacesStatistics);
            } catch (Exception e) {
                // Suppress errors. A malformed element should not fail parsing overall.
            }
            return (J) tree;
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, SpacesStatistics stats) {
            stats.afterTypeCast += hasSpace(typeCast.getExpression().getPrefix());
            return super.visitTypeCast(typeCast, stats);
        }

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
            stats.beforeColonInForEach += hasSpace(forLoop.getControl().getPadding().getVariable().getAfter());
            return super.visitForEachLoop(forLoop, stats);
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, SpacesStatistics stats) {
            stats.beforeFor += hasSpace(forLoop.getControl().getPrefix());
            stats.beforeForSemiColon += hasSpace(forLoop.getControl().getPadding().getInit().get(forLoop.getControl().getInit().size() - 1).getAfter());
            stats.beforeForSemiColon += hasSpace(forLoop.getControl().getPadding().getCondition().getAfter());
            stats.afterForSemiColon += hasSpace(forLoop.getControl().getInit().get(forLoop.getControl().getInit().size() - 1).getPrefix());
            stats.afterForSemiColon += hasSpace(forLoop.getControl().getCondition().getPrefix());
            return super.visitForLoop(forLoop, stats);
        }

        @Override
        public J.If visitIf(J.If iff, SpacesStatistics stats) {
            stats.beforeIf += hasSpace(iff.getIfCondition().getPrefix());
            return super.visitIf(iff, stats);
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, SpacesStatistics stats) {
            List<J> parameters = lambda.getParameters().getParameters();
            if (parameters.size() > 1) {
                List<JRightPadded<J>> paddedParameters = lambda.getParameters().getPadding().getParameters();
                for (int i = 0; i < paddedParameters.size() - 1; i++) {
                    stats.beforeComma += hasSpace(paddedParameters.get(i).getAfter());
                }
                for (int i = 1; i < parameters.size(); i++) {
                    stats.afterComma += hasSpace(parameters.get(i).getPrefix());
                }
            }

            return super.visitLambda(lambda, stats);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, SpacesStatistics stats) {
            stats.beforeMethodDeclaration += hasSpace(method.getPadding().getParameters().getBefore());

            List<Statement> parameters = method.getParameters();
            if (parameters.size() > 1) {
                List<JRightPadded<Statement>> paddedParameters = method.getPadding().getParameters().getPadding().getElements();
                for (int i = 0; i < paddedParameters.size() - 1; i++) {
                    stats.beforeComma += hasSpace(paddedParameters.get(i).getAfter());
                }
                for (int i = 1; i < parameters.size(); i++) {
                    stats.afterComma += hasSpace(parameters.get(i).getPrefix());
                }
            }
            return super.visitMethodDeclaration(method, stats);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, SpacesStatistics stats) {
            stats.beforeMethodCall += hasSpace(method.getPadding().getArguments().getBefore());

            List<Expression> arguments = method.getArguments();
            if (arguments.size() > 1) {
                List<JRightPadded<Expression>> paddedArguments = method.getPadding().getArguments().getPadding().getElements();
                stats.withinMethodCallParentheses += hasSpace(paddedArguments.get(0).getElement().getPrefix());
                stats.withinMethodCallParentheses += hasSpace(paddedArguments.get(method.getArguments().size() - 1).getAfter());
                for (int i = 0; i < paddedArguments.size() - 1; i++) {
                    JRightPadded<Expression> elem = paddedArguments.get(i);
                    stats.beforeComma += hasSpace(elem.getAfter());
                }
                for (int i = 1; i < arguments.size(); i++) {
                    stats.afterComma += hasSpace(arguments.get(i).getPrefix());
                }
            }
            return super.visitMethodInvocation(method, stats);
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, SpacesStatistics stats) {
            JContainer<Expression> initializer = newArray.getPadding().getInitializer();
            List<Expression> elements = newArray.getInitializer();
            if (elements != null && initializer != null && elements.size() > 1) {
                List<JRightPadded<Expression>> paddedElements = initializer.getPadding().getElements();
                for (int i = 0; i < paddedElements.size() - 1; i++) {
                    stats.beforeComma += hasSpace(paddedElements.get(i).getAfter());
                }
                for (int i = 1; i < elements.size(); i++) {
                    stats.afterComma += hasSpace(elements.get(i).getPrefix());
                }
            }
            return super.visitNewArray(newArray, stats);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, SpacesStatistics stats) {
            stats.beforeMethodCall += hasSpace(newClass.getPadding().getArguments().getBefore());

            List<Expression> arguments = newClass.getArguments();
            if (arguments.size() > 1) {
                List<JRightPadded<Expression>> paddedArguments = newClass.getPadding().getArguments().getPadding().getElements();
                stats.withinMethodCallParentheses += hasSpace(paddedArguments.get(0).getElement().getPrefix());
                stats.withinMethodCallParentheses += hasSpace(paddedArguments.get(newClass.getArguments().size() - 1).getAfter());
                for (int i = 0; i < paddedArguments.size() - 1; i++) {
                    JRightPadded<Expression> elem = paddedArguments.get(i);
                    stats.beforeComma += hasSpace(elem.getAfter());
                }
                for (int i = 1; i < arguments.size(); i++) {
                    stats.afterComma += hasSpace(arguments.get(i).getPrefix());
                }
            }
            return super.visitNewClass(newClass, stats);
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
            if (_try.getPadding().getResources() != null) {
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

    private static class WrappingAndBracesStatistics {
        int elseOnNewLine = 0;

        public WrappingAndBracesStyle getWrappingAndBracesStyle() {
            WrappingAndBracesStyle wrappingAndBracesStyle = IntelliJ.wrappingAndBraces();
            return wrappingAndBracesStyle
                    .withIfStatement(new WrappingAndBracesStyle.IfStatement(
                            elseOnNewLine > 0, true, false)
                    );
        }
    }

    private static class FindWrappingAndBracesStyle extends KotlinIsoVisitor<WrappingAndBracesStatistics> {
        @Override
        public J.If.Else visitElse(J.If.Else else_, WrappingAndBracesStatistics stats) {
            stats.elseOnNewLine += hasNewLine(else_.getPrefix());
            return super.visitElse(else_, stats);
        }

        private int hasNewLine(Space space) {
            return space.getWhitespace().contains("\n") ? 1 : -1;
        }
    }
}
