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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
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

    /**
     * Accumulates formatting statistics from sampled source files. Its state is serializable with Jackson and
     * can be {@link #merge(Detector) merged}, so files sampled in separate processes produce the same styles
     * as if one detector had sampled them all.
     */
    @JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    public static class Detector {

        @JsonProperty
        private IndentStatistics indentStatistics = new IndentStatistics();

        @JsonProperty
        private SpacesStatistics spacesStatistics = new SpacesStatistics();

        @JsonProperty
        private WrappingAndBracesStatistics wrappingAndBracesStatistics = new WrappingAndBracesStatistics();

        @JsonProperty
        private GeneralFormatStatistics generalFormatStatistics = new GeneralFormatStatistics();

        @JsonProperty
        private TrailingCommaStatistics trailingCommaStatistics = new TrailingCommaStatistics();

        @JsonProperty
        private ImportLayoutSamples importLayoutSamples = new ImportLayoutSamples();

        private final FindImportLayout findImportLayout = new FindImportLayout();
        private final FindIndentJavaVisitor findIndent = new FindIndentJavaVisitor();
        private final FindSpacesStyle findSpaces = new FindSpacesStyle();
        private final FindWrappingAndBracesStyle findWrappingAndBraces = new FindWrappingAndBracesStyle();
        private final FindLineFormatJavaVisitor findLineFormat = new FindLineFormatJavaVisitor();
        private final FindTrailingCommaVisitor findTrailingComma = new FindTrailingCommaVisitor();

        /**
         * Add another detector's statistics to this one. The result does not depend on the order detectors are merged in.
         *
         * @return this detector
         */
        public Detector merge(Detector other) {
            indentStatistics.merge(other.indentStatistics);
            spacesStatistics.merge(other.spacesStatistics);
            wrappingAndBracesStatistics.merge(other.wrappingAndBracesStatistics);
            generalFormatStatistics.merge(other.generalFormatStatistics);
            trailingCommaStatistics.merge(other.trailingCommaStatistics);
            importLayoutSamples.merge(other.importLayoutSamples);
            return this;
        }

        public void sample(SourceFile cu) {
            if (cu instanceof K.CompilationUnit) {
                findImportLayout.visitNonNull(cu, importLayoutSamples);
                findIndent.visitNonNull(cu, indentStatistics);
                findSpaces.visitNonNull(cu, spacesStatistics);
                findWrappingAndBraces.visitNonNull(cu, wrappingAndBracesStatistics);
                findLineFormat.visitNonNull(cu, generalFormatStatistics);
                findTrailingComma.visitNonNull(cu, trailingCommaStatistics);
            }
        }

        public Autodetect build() {
            TabsAndIndentsStyle tabsAndIndents = indentStatistics.getTabsAndIndentsStyle();
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    tabsAndIndents,
                    importLayoutSamples.getImportLayoutStyle(),
                    spacesStatistics.getSpacesStyle(),
                    wrappingAndBracesStatistics.getWrappingAndBracesStyle(
                            indentStatistics.parametersUseContinuationIndent(tabsAndIndents)),
                    generalFormatStatistics.getFormatStyle(),
                    trailingCommaStatistics.getOtherStyle()
            ));
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class GeneralFormatStatistics {
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        void merge(GeneralFormatStatistics other) {
            linesWithCRLFNewLines += other.linesWithCRLFNewLines;
            linesWithLFNewLines += other.linesWithLFNewLines;
        }

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public GeneralFormatStyle getFormatStyle() {
            boolean useCRLF = !isIndentedWithLFNewLines();

            return new GeneralFormatStyle(useCRLF);
        }
    }

    private static class IndentStatistic {
        @Value
        private static class DepthCoordinate {
            int indentDepth;
            int continuationDepth;
        }

        // depth -> count of whitespace char (4 spaces, 2 tabs, etc) -> count of occurrences
        Map<DepthCoordinate, Map<Integer, Long>> depthToSpaceIndentFrequencies = new ConcurrentHashMap<>();

        void merge(IndentStatistic other) {
            other.depthToSpaceIndentFrequencies.forEach((depth, frequencies) -> {
                Map<Integer, Long> merged = depthToSpaceIndentFrequencies.computeIfAbsent(depth, d -> new ConcurrentHashMap<>());
                frequencies.forEach((charCount, count) -> merged.merge(charCount, count, Long::sum));
            });
        }

        /**
         * Serialized as rows of {@code [indentDepth, continuationDepth, charCount, occurrences]}, since JSON object
         * keys can't be {@link DepthCoordinate}s.
         */
        @JsonValue
        List<long[]> toRows() {
            List<long[]> rows = new ArrayList<>();
            depthToSpaceIndentFrequencies.forEach((depth, frequencies) -> frequencies.forEach((charCount, count) ->
                    rows.add(new long[]{depth.getIndentDepth(), depth.getContinuationDepth(), charCount, count})));
            rows.sort(Comparator.<long[]>comparingLong(r -> r[0]).thenComparingLong(r -> r[1]).thenComparingLong(r -> r[2]));
            return rows;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static IndentStatistic fromRows(List<long[]> rows) {
            IndentStatistic statistic = new IndentStatistic();
            for (long[] row : rows) {
                statistic.depthToSpaceIndentFrequencies
                        .computeIfAbsent(new DepthCoordinate((int) row[0], (int) row[1]), d -> new ConcurrentHashMap<>())
                        .merge((int) row[2], row[3], Long::sum);
            }
            return statistic;
        }

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

            return map.entrySet().stream().max(MOST_FREQUENT_THEN_SMALLEST)
                    .map(Map.Entry::getKey)
                    .orElse(commonIndent * 2);
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class IndentStatistics {
        private IndentStatistic spaceIndentFrequencies = new IndentStatistic();
        private IndentStatistic spaceContinuationIndentFrequencies = new IndentStatistic();
        private IndentStatistic tabIndentFrequencies = new IndentStatistic();
        private IndentStatistic tabContinuationIndentFrequencies = new IndentStatistic();
        private IndentStatistic deltaSpaceIndentFrequencies = new IndentStatistic();
        private long accumulateDepthCount = 0;
        private int multilineAlignedToFirstArgument = 0;
        private int multilineNotAlignedToFirstArgument = 0;
        // Column of a first parameter that begins its own line, relative to its declaration's line -> occurrences
        private Map<Integer, Long> spaceParameterIndentFrequencies = new ConcurrentHashMap<>();
        private Map<Integer, Long> tabParameterIndentFrequencies = new ConcurrentHashMap<>();

        // Position of the visitor within the file currently being sampled
        @Getter
        @JsonIgnore
        private int depth = 0;

        @Getter
        @JsonIgnore
        private int continuationDepth = 1;

        void merge(IndentStatistics other) {
            spaceIndentFrequencies.merge(other.spaceIndentFrequencies);
            spaceContinuationIndentFrequencies.merge(other.spaceContinuationIndentFrequencies);
            tabIndentFrequencies.merge(other.tabIndentFrequencies);
            tabContinuationIndentFrequencies.merge(other.tabContinuationIndentFrequencies);
            deltaSpaceIndentFrequencies.merge(other.deltaSpaceIndentFrequencies);
            accumulateDepthCount += other.accumulateDepthCount;
            multilineAlignedToFirstArgument += other.multilineAlignedToFirstArgument;
            multilineNotAlignedToFirstArgument += other.multilineNotAlignedToFirstArgument;
            other.spaceParameterIndentFrequencies.forEach((offset, count) -> spaceParameterIndentFrequencies.merge(offset, count, Long::sum));
            other.tabParameterIndentFrequencies.forEach((offset, count) -> tabParameterIndentFrequencies.merge(offset, count, Long::sum));
        }

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

        public void recordParameterIndent(String declarationIndent, String parameterIndent) {
            String both = declarationIndent + parameterIndent;
            if (both.contains(" ") && both.contains("\t")) {
                return;
            }
            int offset = parameterIndent.length() - declarationIndent.length();
            if (offset > 0) {
                (both.contains("\t") ? tabParameterIndentFrequencies : spaceParameterIndentFrequencies)
                        .merge(offset, 1L, Long::sum);
            }
        }

        public boolean parametersUseContinuationIndent(TabsAndIndentsStyle style) {
            Map<Integer, Long> frequencies = style.getUseTabCharacter() ? tabParameterIndentFrequencies : spaceParameterIndentFrequencies;
            int charWidth = style.getUseTabCharacter() ? style.getTabSize() : 1;
            return frequencies.entrySet().stream()
                    .max(MOST_FREQUENT_THEN_SMALLEST)
                    .map(entry -> entry.getKey() * charWidth)
                    .map(offset -> offset == style.getContinuationIndent())
                    .orElse(false);
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class TrailingCommaStatistics {
        private long usedTrailingCommaCount = 0;
        private long unusedTrailingCommaCount = 0;

        void merge(TrailingCommaStatistics other) {
            usedTrailingCommaCount += other.usedTrailingCommaCount;
            unusedTrailingCommaCount += other.unusedTrailingCommaCount;
        }

        public OtherStyle getOtherStyle() {
            boolean useTrailingComma = usedTrailingCommaCount > unusedTrailingCommaCount;
            return new OtherStyle(useTrailingComma);
        }
    }

    // Ties go to the smaller key, so the result doesn't depend on map iteration order
    private static final Comparator<Map.Entry<Integer, Long>> MOST_FREQUENT_THEN_SMALLEST =
            Map.Entry.<Integer, Long>comparingByValue()
                    .thenComparing(Map.Entry.<Integer, Long>comparingByKey().reversed());

    private static long getTotalCharCount(IndentStatistic indentStatistic) {
        return indentStatistic.depthToSpaceIndentFrequencies.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().entrySet().stream())
                .mapToLong(entry -> entry.getKey() * entry.getValue())
                .sum();
    }

    private static int getBiggestGroupOfTabSize(IndentStatistic deltaSpaces) {
        Map<Integer, Long> tabSizeToFrequencyMap = deltaSpaces.depthToSpaceIndentFrequencies.entrySet().stream()
                .filter(entry -> entry.getKey().indentDepth != 0)
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(spaceCountToFrequency -> new AbstractMap.SimpleEntry<>(
                                (int) Math.round(spaceCountToFrequency.getKey() / (double) entry.getKey().indentDepth),
                                spaceCountToFrequency.getValue())))
                .collect(groupingBy(Map.Entry::getKey, summingLong(Map.Entry::getValue)));

        return tabSizeToFrequencyMap.entrySet().stream()
                .max(MOST_FREQUENT_THEN_SMALLEST)
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
            Statement firstParameter = method.getParameters().isEmpty() ? null : method.getParameters().get(0);
            String parameterIndent = firstParameter == null || firstParameter instanceof J.Empty ? null : lineIndent(firstParameter.getPrefix());
            if (parameterIndent != null) {
                // Nothing on the opening line to align to: the parameters indent from the line the declaration starts on,
                // which for a primary constructor is the class declaration's
                String declarationIndent = getCursor().getPathAsStream(J.class::isInstance)
                        .map(j -> lineIndent(((J) j).getPrefix()))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("");
                stats.recordParameterIndent(declarationIndent, parameterIndent);
            }
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
            method.getLeadingAnnotations().forEach(a -> visit(a, stats));
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
            vd.getPadding().getVariables().forEach(t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, stats));
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

        private static @Nullable String lineIndent(Space prefix) {
            return prefix.getLastWhitespace().contains("\n") ? prefix.getIndent() : null;
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

    /**
     * Which import layout group an import counts toward.
     */
    private enum ImportCategory {
        ALIAS,
        JAVA,
        JAVAX,
        KOTLIN,
        OTHER;

        static ImportCategory of(J.Import anImport) {
            if (anImport.getAlias() != null) {
                return ALIAS;
            }
            String packageName = anImport.getPackageName();
            if (packageName.startsWith("java.")) {
                return JAVA;
            } else if (packageName.startsWith("javax.")) {
                return JAVAX;
            } else if (packageName.startsWith("kotlin.")) {
                return KOTLIN;
            }
            return OTHER;
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class ImportLayoutSamples {
        List<List<ImportCategory>> importsBySourceFile = new ArrayList<>();
        // Not yet used to build the import layout style
        int minimumFoldedImports = Integer.MAX_VALUE;
        int minimumFoldedStaticImports = Integer.MAX_VALUE;

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

        void merge(ImportLayoutSamples other) {
            importsBySourceFile.addAll(other.importsBySourceFile);
            minimumFoldedImports = Math.min(minimumFoldedImports, other.minimumFoldedImports);
            minimumFoldedStaticImports = Math.min(minimumFoldedStaticImports, other.minimumFoldedStaticImports);
        }

        private static int compareCategorySequences(List<ImportCategory> a, List<ImportCategory> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int c = a.get(i).compareTo(b.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(a.size(), b.size());
        }

        public ImportLayoutStyle getImportLayoutStyle() {
            return importsBySourceFile.stream()
                    // a total order, so the choice doesn't depend on the order files were sampled in
                    .max(Comparator.<List<ImportCategory>, Integer>comparing(List::size)
                            .thenComparing(ImportLayoutSamples::compareCategorySequences))
                    .map(longestImports -> {
                        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
                        builder.packageToFold("kotlinx.android.synthetic.*", true)
                                .packageToFold("io.ktor.*", true);

                        Map<String, List<Integer>> weightMap = new HashMap<>();

                        for (int i = 0; i < longestImports.size(); i++) {
                            int weight = longestImports.size() - i;
                            switch (longestImports.get(i)) {
                                case ALIAS:
                                    weightMap.computeIfAbsent(TYPE_ALL_ALIASES, k -> new ArrayList<>()).add(weight);
                                    break;
                                case JAVA:
                                    weightMap.computeIfAbsent(TYPE_JAVA, k -> new ArrayList<>()).add(weight);
                                    break;
                                case JAVAX:
                                    weightMap.computeIfAbsent(TYPE_JAVAX, k -> new ArrayList<>()).add(weight);
                                    break;
                                case KOTLIN:
                                    weightMap.computeIfAbsent(TYPE_KOTLIN, k -> new ArrayList<>()).add(weight);
                                    break;
                                default:
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
    }

    private static class FindImportLayout extends KotlinIsoVisitor<ImportLayoutSamples> {
        @Override
        public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ImportLayoutSamples samples) {
            for (J.Import anImport : cu.getImports()) {
                if (anImport.getQualid().getTarget() instanceof J.Empty) {
                    // skip unqualified imports
                    continue;
                }
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (anImport.isStatic()) {
                        String importedTypeName = anImport.getTypeName();
                        Set<String> staticMembers = new HashSet<>();
                        for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                            JavaType.FullyQualified owner = TypeUtils.asFullyQualified(variable.getOwner());
                            if (variable.hasFlags(Flag.Static) &&
                                owner != null && importedTypeName.equals(owner.getFullyQualifiedName())) {
                                staticMembers.add(variable.getName());
                            }
                        }
                        for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                            if (method.hasFlags(Flag.Static) &&
                                importedTypeName.equals(method.getDeclaringType().getFullyQualifiedName())) {
                                staticMembers.add(method.getName());
                            }
                        }

                        samples.minimumFoldedStaticImports = Math.min(
                                samples.minimumFoldedStaticImports,
                                staticMembers.size()
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

                        samples.minimumFoldedImports = Math.min(
                                samples.minimumFoldedImports,
                                fqns.size()
                        );
                    }
                }
            }

            samples.importsBySourceFile.add(cu.getImports().stream()
                    // skip unqualified imports
                    .filter(i -> !(i.getQualid().getTarget() instanceof J.Empty))
                    .map(ImportCategory::of)
                    .collect(toList()));
            return cu;
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class SpacesStatistics {
        // Net votes: +1 for each occurrence with a space, -1 for each without
        int beforeIf;
        int beforeMethodCall;
        int beforeMethodDeclaration;
        int beforeFor;
        int beforeWhile;
        int beforeSwitch;
        int beforeTry;
        int beforeCatch;
        int beforeSynchronized;
        int beforeComma;
        int afterComma;
        int beforeColonInForEach;
        int beforeForSemiColon;
        int afterForSemiColon;
        int afterTypeCast;
        int withinMethodCallParentheses;

        void merge(SpacesStatistics other) {
            beforeIf += other.beforeIf;
            beforeMethodCall += other.beforeMethodCall;
            beforeMethodDeclaration += other.beforeMethodDeclaration;
            beforeFor += other.beforeFor;
            beforeWhile += other.beforeWhile;
            beforeSwitch += other.beforeSwitch;
            beforeTry += other.beforeTry;
            beforeCatch += other.beforeCatch;
            beforeSynchronized += other.beforeSynchronized;
            beforeComma += other.beforeComma;
            afterComma += other.afterComma;
            beforeColonInForEach += other.beforeColonInForEach;
            beforeForSemiColon += other.beforeForSemiColon;
            afterForSemiColon += other.afterForSemiColon;
            afterTypeCast += other.afterTypeCast;
            withinMethodCallParentheses += other.withinMethodCallParentheses;
        }

        public SpacesStyle getSpacesStyle() {
            SpacesStyle spaces = IntelliJ.spaces();
            SpacesStyle.BeforeParentheses bp = spaces.getBeforeParentheses();
            SpacesStyle.Other other = spaces.getOther();
            boolean forOrWhile = vote(beforeFor, bp.getForParentheses()) || vote(beforeWhile, bp.getWhileParentheses());
            return spaces
                    .withBeforeParentheses(
                            new SpacesStyle.BeforeParentheses(
                                    vote(beforeIf, bp.getIfParentheses()),
                                    forOrWhile,
                                    forOrWhile,
                                    vote(beforeTry, bp.getCatchParentheses()) || vote(beforeCatch, bp.getCatchParentheses()),
                                    true
                            )
                    )
                    .withOther(new SpacesStyle.Other(
                            vote(beforeComma, other.getBeforeComma()),
                            vote(afterComma, other.getAfterComma()),
                            false, true, true, true, true, true, true, true
                    ));
        }
    }

    /**
     * Without a majority either way, fall back to the default.
     */
    private static boolean vote(int net, boolean defaultValue) {
        return net > 0 || net == 0 && defaultValue;
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

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    private static class WrappingAndBracesStatistics {
        int elseOnNewLine = 0;

        void merge(WrappingAndBracesStatistics other) {
            elseOnNewLine += other.elseOnNewLine;
        }

        public WrappingAndBracesStyle getWrappingAndBracesStyle(boolean parametersUseContinuationIndent) {
            WrappingAndBracesStyle wrappingAndBracesStyle = IntelliJ.wrappingAndBraces();
            return wrappingAndBracesStyle
                    .withFunctionDeclarationParameters(wrappingAndBracesStyle.getFunctionDeclarationParameters()
                            .withUseContinuationIndent(parametersUseContinuationIndent))
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
