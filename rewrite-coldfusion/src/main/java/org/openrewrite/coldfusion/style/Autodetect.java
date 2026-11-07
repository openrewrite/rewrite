/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.marker.FunctionKeyword;
import org.openrewrite.coldfusion.marker.OmitComma;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;

import static java.util.Collections.emptySet;

/**
 * Styles detected from a repository's own CFML: how far it indents and with what, where it puts spaces, braces and
 * blank lines. Each setting is what most of the repository does, and where the repository gives no evidence, it is
 * CFFormat's. Settings that limit what a formatter keeps, as the most blank lines in a row, are set so that the
 * repository's own files rarely exceed them.
 */
public class Autodetect extends NamedStyles {

    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.coldfusion.style.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing CFML.",
                emptySet(), styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static class Detector {
        private final Statistics statistics = new Statistics();

        public void sample(SourceFile sourceFile) {
            if (sourceFile instanceof Cf.CompilationUnit) {
                new FindStyles().visit(sourceFile, statistics);
                lineBreaks(((Cf.CompilationUnit) sourceFile).printAll(), statistics);
            }
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    statistics.tabsAndIndents(),
                    statistics.spaces(),
                    statistics.blankLines(),
                    statistics.wrappingAndBraces(),
                    new GeneralFormatStyle(statistics.crlf > statistics.lf)
            ));
        }

        private static void lineBreaks(String source, Statistics statistics) {
            for (int i = 0; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '\n') {
                    statistics.lf++;
                } else if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') {
                    statistics.crlf++;
                    i++;
                }
            }
        }
    }

    /**
     * Occurrences of a setting written one way or the other.
     */
    private static class Count {
        long on;
        long off;

        void add(boolean value) {
            add(value, 1);
        }

        void add(boolean value, long weight) {
            if (value) {
                on += weight;
            } else {
                off += weight;
            }
        }

        boolean or(boolean otherwise) {
            return on == off ? otherwise : on > off;
        }
    }

    /**
     * Occurrences of each value of a number.
     */
    private static class Histogram {
        final Map<Integer, Long> counts = new TreeMap<>();
        long total;

        void add(int value) {
            counts.merge(value, 1L, Long::sum);
            total++;
        }

        @Nullable
        Integer mostCommon() {
            return counts.entrySet().stream().max(Map.Entry.<Integer, Long>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder()))).map(Map.Entry::getKey).orElse(null);
        }

        /**
         * The least value no more than a small share of the occurrences exceed.
         */
        int ceiling(int floor, double share) {
            long allowed = (long) Math.floor(total * share);
            long above = total;
            for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
                above -= entry.getValue();
                if (entry.getKey() >= floor && above <= allowed) {
                    return entry.getKey();
                }
            }
            return floor;
        }

        /**
         * The greatest value no more than a small share of the occurrences fall below.
         */
        int floor(double share) {
            long allowed = (long) Math.floor(total * share);
            long below = 0;
            int result = 0;
            for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
                if (below > allowed) {
                    break;
                }
                result = entry.getKey();
                below += entry.getValue();
            }
            return result;
        }

        /**
         * The least value occurring in at least a share of the occurrences.
         */
        @Nullable
        Integer leastCommon(double share) {
            for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
                if (entry.getValue() >= total * share) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    private static class Statistics {
        long lf;
        long crlf;

        long tabLines;
        long spaceLines;
        final Histogram spaceIndents = new Histogram();
        final Histogram continuations = new Histogram();
        final Count caseFromSwitch = new Count();
        final Count componentBody = new Count();
        final Count scriptBody = new Count();
        final Count silentBody = new Count();
        final Count tagCaseFromSwitch = new Count();
        final Count catchFromTry = new Count();

        final Count keywordParenthesesBefore = new Count();
        final Count functionDeclarationParenthesesBefore = new Count();
        final Count functionCallParenthesesBefore = new Count();
        final Count anonymousFunctionParenthesesBefore = new Count();
        final Count binary = new Count();
        final Count assignment = new Count();
        final Count ternary = new Count();
        final Count arrow = new Count();
        final Count keywordParentheses = new Count();
        final Count functionDeclarationParentheses = new Count();
        final Count emptyFunctionDeclarationParentheses = new Count();
        final Count functionCallParentheses = new Count();
        final Count emptyFunctionCallParentheses = new Count();
        final Count anonymousFunctionParentheses = new Count();
        final Count groupingParentheses = new Count();
        final Count brackets = new Count();
        final Count arrayLiteral = new Count();
        final Count emptyArrayLiteral = new Count();
        final Count structLiteral = new Count();
        final Count emptyStructLiteral = new Count();
        final Count beforeComma = new Count();
        final Count afterComma = new Count();
        final Count beforeForSemicolon = new Count();
        final Count afterForSemicolon = new Count();
        final Count beforeColon = new Count();
        final Count afterColon = new Count();
        final Count keyValueEquals = new Count();
        final Count beforeComponentBrace = new Count();
        final Count beforeFunctionBrace = new Count();
        final Count indentedEmptyLines = new Count();
        final Count beforeClosureBrace = new Count();
        final Count beforeStatementBrace = new Count();
        final Count beforeKeywordBrace = new Count();
        final Count alignedAssignments = new Count();
        final Count alignedAttributes = new Count();
        final Count beforeKeywordAfterBlock = new Count();
        final Count attributeEquals = new Count();
        final Count beforeSelfClosingEnd = new Count();
        final Count beforeTagEnd = new Count();

        long oneLineBlocks;
        final Count componentBraceOnNewLine = new Count();
        final Count functionBraceOnNewLine = new Count();
        final Count closingBracketsIndented = new Count();
        final Count metadataEquals = new Count();
        final Count statementBraceOnNewLine = new Count();
        final Count elseOnNewLine = new Count();
        final Count catchOnNewLine = new Count();
        final Count whileOnNewLine = new Count();

        final Histogram blankLinesInCode = new Histogram();
        final Histogram blankLinesInDeclarations = new Histogram();
        final Histogram blankLinesBeforeEnd = new Histogram();
        final Histogram blankLinesAroundFunction = new Histogram();

        TabsAndIndentsStyle tabsAndIndents() {
            TabsAndIndentsStyle defaults = CFFormat.tabsAndIndents();
            boolean tabs = tabLines > spaceLines;
            int indent = defaults.getIndentSize();
            if (!tabs) {
                Integer common = spaceIndents.mostCommon();
                if (common != null && common > 0 && common <= 8) {
                    indent = common;
                }
            }
            int tabSize = tabs ? defaults.getTabSize() : indent;
            Integer continuation = continuations.leastCommon(0.1);
            int continuationIndent = continuation == null || continuation > 2 * indent ? indent : continuation;
            TabsAndIndentsStyle.Tags tags = defaults.getTags();
            return new TabsAndIndentsStyle(tabs, tabSize, indent, continuationIndent,
                    indentedEmptyLines.or(defaults.getKeepIndentsOnEmptyLines()),
                    caseFromSwitch.or(defaults.getIndentCaseFromSwitch()),
                    closingBracketsIndented.or(defaults.getIndentClosingBrackets()),
                    new TabsAndIndentsStyle.Tags(
                            componentBody.or(tags.getIndentComponentBody()),
                            scriptBody.or(tags.getIndentScriptBody()),
                            silentBody.or(tags.getIndentSilentBody()),
                            tagCaseFromSwitch.or(tags.getIndentCaseFromSwitch()),
                            catchFromTry.or(tags.getIndentCatchFromTry())));
        }

        SpacesStyle spaces() {
            SpacesStyle d = CFFormat.spaces();
            return new SpacesStyle(
                    new SpacesStyle.BeforeParentheses(
                            keywordParenthesesBefore.or(d.getBeforeParentheses().getKeywords()),
                            functionDeclarationParenthesesBefore.or(d.getBeforeParentheses().getFunctionDeclaration()),
                            functionCallParenthesesBefore.or(d.getBeforeParentheses().getFunctionCall()),
                            anonymousFunctionParenthesesBefore.or(d.getBeforeParentheses().getAnonymousFunction())),
                    new SpacesStyle.AroundOperators(
                            binary.or(d.getAroundOperators().getBinary()),
                            assignment.or(d.getAroundOperators().getAssignment()),
                            ternary.or(d.getAroundOperators().getTernary()),
                            arrow.or(d.getAroundOperators().getArrow())),
                    new SpacesStyle.Within(
                            keywordParentheses.or(d.getWithin().getKeywordParentheses()),
                            functionDeclarationParentheses.or(d.getWithin().getFunctionDeclarationParentheses()),
                            emptyFunctionDeclarationParentheses.or(d.getWithin().getEmptyFunctionDeclarationParentheses()),
                            functionCallParentheses.or(d.getWithin().getFunctionCallParentheses()),
                            emptyFunctionCallParentheses.or(d.getWithin().getEmptyFunctionCallParentheses()),
                            anonymousFunctionParentheses.or(d.getWithin().getAnonymousFunctionParentheses()),
                            groupingParentheses.or(d.getWithin().getGroupingParentheses()),
                            brackets.or(d.getWithin().getBrackets()),
                            arrayLiteral.or(d.getWithin().getArrayLiteral()),
                            emptyArrayLiteral.or(d.getWithin().getEmptyArrayLiteral()),
                            structLiteral.or(d.getWithin().getStructLiteral()),
                            emptyStructLiteral.or(d.getWithin().getEmptyStructLiteral())),
                    new SpacesStyle.Other(
                            beforeComma.or(d.getOther().getBeforeComma()),
                            afterComma.or(d.getOther().getAfterComma()),
                            beforeForSemicolon.or(d.getOther().getBeforeForSemicolon()),
                            afterForSemicolon.or(d.getOther().getAfterForSemicolon()),
                            beforeColon.or(d.getOther().getBeforeColon()),
                            afterColon.or(d.getOther().getAfterColon()),
                            keyValueEquals.or(d.getOther().getAroundKeyValueEquals()),
                            metadataEquals.or(d.getOther().getAroundMetadataEquals()),
                            beforeComponentBrace.or(d.getOther().getBeforeComponentBrace()),
                            beforeFunctionBrace.or(d.getOther().getBeforeFunctionBrace()),
                            beforeClosureBrace.or(d.getOther().getBeforeClosureBrace()),
                            beforeStatementBrace.or(d.getOther().getBeforeStatementBrace()),
                            beforeKeywordBrace.or(d.getOther().getBeforeKeywordBrace()),
                            beforeKeywordAfterBlock.or(d.getOther().getBeforeKeywordAfterBlock()),
                            alignedAssignments.on >= 0.1 * (alignedAssignments.on + alignedAssignments.off) && alignedAssignments.on > 0),
                    new SpacesStyle.Tags(
                            attributeEquals.or(d.getTags().getAroundAttributeEquals()),
                            alignedAttributes.on >= 0.1 * (alignedAttributes.on + alignedAttributes.off) && alignedAttributes.on > 0,
                            beforeSelfClosingEnd.or(d.getTags().getBeforeSelfClosingEnd()),
                            beforeTagEnd.or(d.getTags().getBeforeTagEnd())));
        }

        BlankLinesStyle blankLines() {
            BlankLinesStyle d = CFFormat.blankLines();
            return new BlankLinesStyle(
                    new BlankLinesStyle.KeepMaximum(
                            blankLinesInDeclarations.total == 0 ? d.getKeepMaximum().getInDeclarations() : blankLinesInDeclarations.ceiling(1, 0.005),
                            blankLinesInCode.total == 0 ? d.getKeepMaximum().getInCode() : blankLinesInCode.ceiling(1, 0.005),
                            blankLinesBeforeEnd.total == 0 ? d.getKeepMaximum().getBeforeEndOfBlock() : blankLinesBeforeEnd.ceiling(0, 0.005)),
                    new BlankLinesStyle.Minimum(
                            blankLinesAroundFunction.total == 0 ? d.getMinimum().getAroundFunction() :
                                    Math.min(1, blankLinesAroundFunction.floor(0.02))));
        }

        WrappingAndBracesStyle wrappingAndBraces() {
            WrappingAndBracesStyle d = CFFormat.wrappingAndBraces();
            return new WrappingAndBracesStyle(
                    oneLineBlocks > 0 || d.getKeepSimpleBlocksInOneLine(),
                    new WrappingAndBracesStyle.Braces(
                            componentBraceOnNewLine.or(d.getBraces().getComponentOnNewLine()),
                            functionBraceOnNewLine.or(d.getBraces().getFunctionOnNewLine()),
                            statementBraceOnNewLine.or(d.getBraces().getStatementOnNewLine())),
                    new WrappingAndBracesStyle.KeywordsAfterBlock(
                            elseOnNewLine.or(d.getKeywordsAfterBlock().getElseOnNewLine()),
                            catchOnNewLine.or(d.getKeywordsAfterBlock().getCatchOnNewLine()),
                            whileOnNewLine.or(d.getKeywordsAfterBlock().getWhileOnNewLine())));
        }
    }

    private static class FindStyles extends ColdFusionIsoVisitor<Statistics> {
        private static final String INDENT = "org.openrewrite.coldfusion.style.indent";

        @Override
        public @Nullable J preVisit(J tree, Statistics s) {
            Space prefix = tree.getPrefix();
            if (Layout.hasLineBreak(prefix) && !(tree instanceof Cf.ExpressionStatement)) {
                String indent = indent(prefix);
                getCursor().putMessage(INDENT, indent);
                continuation(tree, indent, s);
            }
            return super.preVisit(tree, s);
        }

        /**
         * A line that continues a statement, measured from the line it continues.
         */
        private void continuation(J tree, String indent, Statistics s) {
            Cursor parent = getCursor().getParentTreeCursor();
            Object owner = parent.getValue();
            if (owner instanceof Cf.ExpressionStatement || tree instanceof Statement && isListed(owner, tree) ||
                tree instanceof J.Block || tree instanceof J.If.Else || tree instanceof J.Try.Catch || tree instanceof J.Empty ||
                tree instanceof Cf.Text || tree instanceof Cf.Interpolation || inTag(getCursor())) {
                return;
            }
            int relative = width(indent) - width(lineOf(parent));
            if (relative > 0) {
                s.continuations.add(relative);
            }
        }

        private static boolean isListed(Object owner, J tree) {
            if (owner instanceof J.Block) {
                return ((J.Block) owner).getStatements().contains(tree);
            }
            if (owner instanceof J.Case) {
                return ((J.Case) owner).getStatements().contains(tree);
            }
            if (owner instanceof J.If || owner instanceof J.If.Else || owner instanceof J.ForLoop ||
                owner instanceof J.ForEachLoop || owner instanceof J.WhileLoop || owner instanceof J.DoWhileLoop) {
                return true;
            }
            return owner instanceof Cf.CompilationUnit || owner instanceof J.MethodDeclaration && TemplateOutput.isTagSyntax((J) owner);
        }

        /**
         * Whether the cursor is in a tag's attributes, which authors align rather than indent.
         */
        private static boolean inTag(Cursor cursor) {
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                Object value = c.getValue();
                if (value instanceof Cf.TagAttribute || value instanceof J.Annotation) {
                    return true;
                }
                if (value instanceof Statement && !(value instanceof Cf.Tag)) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public J.Block visitBlock(J.Block block, Statistics s) {
            J.Block b = super.visitBlock(block, s);
            Cursor ownerCursor = getCursor().getParentTreeCursor();
            Object owner = ownerCursor.getValue();
            boolean template = TemplateOutput.isTemplateBody(getCursor());
            List<Statement> statements = b.getStatements();
            boolean hasText = statements.stream().anyMatch(Cf.Text.class::isInstance);
            String line = owner instanceof J.Block || owner instanceof Cf.CompilationUnit ? lineOf(getCursor()) : lineOf(ownerCursor);
            boolean oneLine = !statements.isEmpty() && !Layout.hasLineBreak(b.getEnd());
            boolean whitespace = !b.getEnd().isEmpty();
            boolean componentBody = owner instanceof J.ClassDeclaration;
            // a body's convention counts for as many lines as the body moves when it changes
            long weight = -1;
            for (int i = 0; i < statements.size(); i++) {
                Statement statement = statements.get(i);
                Space prefix = statement.getPrefix();
                oneLine &= !Layout.hasLineBreak(prefix);
                whitespace |= !prefix.isEmpty();
                if (!Layout.hasLineBreak(prefix) || statement instanceof Cf.Text || template && hasText) {
                    continue;
                }
                blankLines(prefix, componentBody ? s.blankLinesInDeclarations : s.blankLinesInCode);
                emptyLines(prefix, s);
                if (componentBody && i > 0 && (statement instanceof J.MethodDeclaration || statements.get(i - 1) instanceof J.MethodDeclaration)) {
                    s.blankLinesAroundFunction.add(blankLinesBefore(prefix));
                }
                String indent = indent(prefix);
                characters(indent, s);
                int relative = width(indent) - width(line);
                if (owner instanceof J.Switch) {
                    weight = vote(s.caseFromSwitch, relative > 0, weight, b);
                } else if (owner instanceof Cf.Tag && ((Cf.Tag) owner).isNamed("cfscript")) {
                    weight = vote(s.scriptBody, relative > 0, weight, b);
                } else if (owner instanceof Cf.Tag && ((Cf.Tag) owner).isNamed("cfsilent")) {
                    weight = vote(s.silentBody, relative > 0, weight, b);
                } else if (componentBody && TemplateOutput.isTagSyntax((J) owner)) {
                    weight = vote(s.componentBody, relative > 0, weight, b);
                } else if (owner instanceof Cf.Tag && statement instanceof Cf.Tag && isSpecial((Cf.Tag) owner, (Cf.Tag) statement)) {
                    Cf.Tag tag = (Cf.Tag) owner;
                    if (tag.isNamed("cfswitch")) {
                        weight = vote(s.tagCaseFromSwitch, relative > 0, weight, b);
                    } else if (tag.isNamed("cftry")) {
                        weight = vote(s.catchFromTry, relative > 0, weight, b);
                    }
                } else if (relative > 0 && indent.indexOf('\t') < 0 && line.indexOf('\t') < 0) {
                    s.spaceIndents.add(relative);
                }
            }
            if (!statements.isEmpty() && !template && Layout.hasLineBreak(b.getEnd())) {
                blankLines(b.getEnd(), s.blankLinesBeforeEnd);
            }
            if (!template && oneLine && whitespace) {
                s.oneLineBlocks++;
            }
            return b;
        }

        private static long vote(Count count, boolean value, long weight, J.Block body) {
            if (weight < 0) {
                long lines = lines(body);
                count.add(value, lines);
                return lines;
            }
            return weight;
        }

        /**
         * The lines a body spans, as the line breaks in its whitespace count them.
         */
        private static long lines(J.Block body) {
            long[] lines = {1};
            new ColdFusionIsoVisitor<Integer>() {
                @Override
                public Space visitSpace(Space space, Space.Location loc, Integer p) {
                    lines[0] += Layout.lineBreaks(space.getWhitespace());
                    for (Comment comment : space.getComments()) {
                        lines[0] += Layout.lineBreaks(comment.getSuffix());
                    }
                    return space;
                }

                @Override
                public Space visitSpace(Space space, CfSpace.Location loc, Integer p) {
                    return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
                }
            }.visit(body, 0);
            return lines[0];
        }

        private static boolean isSpecial(Cf.Tag owner, Cf.Tag child) {
            return owner.isNamed("cfswitch") && (child.isNamed("cfcase") || child.isNamed("cfdefaultcase")) ||
                   owner.isNamed("cftry") && (child.isNamed("cfcatch") || child.isNamed("cffinally"));
        }

        private static void characters(String indent, Statistics s) {
            if (indent.startsWith("\t")) {
                s.tabLines++;
            } else if (indent.startsWith(" ")) {
                s.spaceLines++;
            }
        }

        // ----------------------------------------------------------------------------------
        // Spaces
        // ----------------------------------------------------------------------------------

        @Override
        public J.Binary visitBinary(J.Binary binary, Statistics s) {
            J.Binary b = super.visitBinary(binary, s);
            if (!b.getMarkers().findFirst(OperatorSpelling.class).isPresent()) {
                count(s.binary, b.getPadding().getOperator().getBefore());
                count(s.binary, b.getRight().getPrefix());
            }
            return b;
        }

        @Override
        public Cf.Binary visitBinary(Cf.Binary binary, Statistics s) {
            Cf.Binary b = super.visitBinary(binary, s);
            Cf.Binary.Type type = b.getOperator();
            boolean word = type == Cf.Binary.Type.Contains || type == Cf.Binary.Type.DoesNotContain ||
                           type == Cf.Binary.Type.Equivalence || type == Cf.Binary.Type.Implication || type == Cf.Binary.Type.Xor;
            if (!word && !b.getMarkers().findFirst(OperatorSpelling.class).isPresent()) {
                count(s.binary, b.getPadding().getOperator().getBefore());
                count(s.binary, b.getRight().getPrefix());
            }
            return b;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, Statistics s) {
            J.Assignment a = super.visitAssignment(assignment, s);
            aligned(s.alignedAssignments, a.getPadding().getAssignment().getBefore());
            count(s.assignment, a.getPadding().getAssignment().getBefore());
            count(s.assignment, a.getAssignment().getPrefix());
            return a;
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Statistics s) {
            J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, s);
            count(s.assignment, a.getPadding().getOperator().getBefore());
            count(s.assignment, a.getAssignment().getPrefix());
            return a;
        }

        @Override
        public Cf.AssignmentOperation visitAssignmentOperation(Cf.AssignmentOperation assignOp, Statistics s) {
            Cf.AssignmentOperation a = super.visitAssignmentOperation(assignOp, s);
            count(s.assignment, a.getPadding().getOperator().getBefore());
            count(s.assignment, a.getAssignment().getPrefix());
            return a;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Statistics s) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, s);
            JLeftPadded<Expression> initializer = v.getPadding().getInitializer();
            if (initializer != null) {
                aligned(s.alignedAssignments, initializer.getBefore());
                count(s.assignment, initializer.getBefore());
                count(s.assignment, initializer.getElement().getPrefix());
            }
            return v;
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, Statistics s) {
            J.Ternary t = super.visitTernary(ternary, s);
            count(s.ternary, t.getPadding().getTruePart().getBefore());
            count(s.ternary, t.getTruePart().getPrefix());
            count(s.ternary, t.getPadding().getFalsePart().getBefore());
            count(s.ternary, t.getFalsePart().getPrefix());
            return t;
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, Statistics s) {
            J.Lambda l = super.visitLambda(lambda, s);
            J.Lambda.Parameters parameters = l.getParameters();
            if (l.getMarkers().findFirst(FunctionKeyword.class).isPresent()) {
                count(s.anonymousFunctionParenthesesBefore, parameters.getPrefix());
                if (l.getBody() instanceof J.Block) {
                    count(s.beforeClosureBrace, l.getBody().getPrefix());
                }
            } else {
                count(s.arrow, l.getArrow());
                count(s.arrow, l.getBody().getPrefix());
            }
            if (parameters.isParenthesized()) {
                delimited(parameters.getPadding().getParameters(), s.anonymousFunctionParentheses, s.emptyFunctionDeclarationParentheses, s);
            }
            return l;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Statistics s) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, s);
            if (TemplateOutput.isTagSyntax(m)) {
                tagSyntax(m.getMarkers(), s);
                String line = lineOf(getCursor());
                for (Statement parameter : m.getParameters()) {
                    if (Layout.hasLineBreak(parameter.getPrefix())) {
                        String indent = indent(parameter.getPrefix());
                        characters(indent, s);
                        int relative = width(indent) - width(line);
                        if (relative > 0 && indent.indexOf('\t') < 0 && line.indexOf('\t') < 0) {
                            s.spaceIndents.add(relative);
                        }
                    }
                }
                return m;
            }
            JContainer<Statement> parameters = m.getPadding().getParameters();
            count(s.functionDeclarationParenthesesBefore, parameters.getBefore());
            delimited(parameters.getPadding().getElements(), s.functionDeclarationParentheses, s.emptyFunctionDeclarationParentheses, s);
            if (m.getBody() != null) {
                brace(m.getBody().getPrefix(), s.beforeFunctionBrace, s.functionBraceOnNewLine);
            }
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Statistics s) {
            J.MethodInvocation m = super.visitMethodInvocation(method, s);
            JContainer<Expression> arguments = m.getPadding().getArguments();
            if (!arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                count(s.functionCallParenthesesBefore, arguments.getBefore());
                delimited(arguments.getPadding().getElements(), s.functionCallParentheses, s.emptyFunctionCallParentheses, s);
            }
            return m;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, Statistics s) {
            J.NewClass n = super.visitNewClass(newClass, s);
            JContainer<Expression> arguments = n.getPadding().getArguments();
            if (!arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                count(s.functionCallParenthesesBefore, arguments.getBefore());
                delimited(arguments.getPadding().getElements(), s.functionCallParentheses, s.emptyFunctionCallParentheses, s);
            }
            return n;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, Statistics s) {
            J.ArrayAccess a = super.visitArrayAccess(arrayAccess, s);
            JRightPadded<Expression> index = a.getDimension().getPadding().getIndex();
            count(s.brackets, index.getElement().getPrefix());
            count(s.brackets, index.getAfter());
            return a;
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, Statistics s) {
            J.NewArray n = super.visitNewArray(newArray, s);
            JContainer<Expression> initializer = n.getPadding().getInitializer();
            if (initializer != null && n.getTypeExpression() == null) {
                delimited(initializer.getPadding().getElements(), s.arrayLiteral, s.emptyArrayLiteral, s);
            }
            return n;
        }

        @Override
        public Cf.StructLiteral visitStructLiteral(Cf.StructLiteral structLiteral, Statistics s) {
            Cf.StructLiteral st = super.visitStructLiteral(structLiteral, s);
            List<Expression> entries = st.getEntries();
            if (!(st.isOrdered() && entries.size() == 1 && entries.get(0) instanceof Cf.KeyValue &&
                  ((Cf.KeyValue) entries.get(0)).getKey() instanceof J.Empty)) {
                delimited(st.getPadding().getEntries().getPadding().getElements(), s.structLiteral, s.emptyStructLiteral, s);
            }
            return st;
        }

        @Override
        public Cf.KeyValue visitKeyValue(Cf.KeyValue keyValue, Statistics s) {
            Cf.KeyValue k = super.visitKeyValue(keyValue, s);
            if (k.getKey() instanceof J.Empty) {
                return k;
            }
            aligned(s.alignedAssignments, k.getPadding().getSeparator().getBefore());
            if (k.getSeparator() == Cf.KeyValue.Separator.Colon) {
                count(s.beforeColon, k.getPadding().getSeparator().getBefore());
                count(s.afterColon, k.getValue().getPrefix());
            } else {
                count(s.keyValueEquals, k.getPadding().getSeparator().getBefore());
                count(s.keyValueEquals, k.getValue().getPrefix());
            }
            return k;
        }

        @Override
        public Cf.Invocation visitInvocation(Cf.Invocation invocation, Statistics s) {
            Cf.Invocation i = super.visitInvocation(invocation, s);
            delimited(i.getPadding().getArguments().getPadding().getElements(), s.functionCallParentheses, s.emptyFunctionCallParentheses, s);
            return i;
        }

        @Override
        public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, Statistics s) {
            J.Parentheses<T> p = super.visitParentheses(parens, s);
            count(s.groupingParentheses, p.getPadding().getTree().getElement().getPrefix());
            count(s.groupingParentheses, p.getPadding().getTree().getAfter());
            return p;
        }

        @Override
        public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, Statistics s) {
            J.ControlParentheses<T> c = super.visitControlParentheses(controlParens, s);
            count(s.keywordParenthesesBefore, c.getPrefix());
            count(s.keywordParentheses, c.getPadding().getTree().getElement().getPrefix());
            count(s.keywordParentheses, c.getPadding().getTree().getAfter());
            return c;
        }

        @Override
        public J.ForLoop.Control visitForControl(J.ForLoop.Control control, Statistics s) {
            J.ForLoop.Control c = super.visitForControl(control, s);
            count(s.keywordParenthesesBefore, c.getPrefix());
            List<JRightPadded<Statement>> init = c.getPadding().getInit();
            if (!init.isEmpty() && !(init.get(init.size() - 1).getElement() instanceof J.Empty)) {
                count(s.beforeForSemicolon, init.get(init.size() - 1).getAfter());
            }
            JRightPadded<Expression> condition = c.getPadding().getCondition();
            if (!(condition.getElement() instanceof J.Empty)) {
                count(s.afterForSemicolon, condition.getElement().getPrefix());
                count(s.beforeForSemicolon, condition.getAfter());
            }
            List<JRightPadded<Statement>> update = c.getPadding().getUpdate();
            if (!update.isEmpty() && !(update.get(0).getElement() instanceof J.Empty)) {
                count(s.afterForSemicolon, update.get(0).getElement().getPrefix());
            }
            return c;
        }

        @Override
        public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, Statistics s) {
            J.ForEachLoop.Control c = super.visitForEachControl(control, s);
            count(s.keywordParenthesesBefore, c.getPrefix());
            count(s.keywordParentheses, c.getPadding().getVariable().getElement().getPrefix());
            count(s.keywordParentheses, c.getPadding().getIterable().getAfter());
            return c;
        }

        @Override
        public J.If visitIf(J.If iff, Statistics s) {
            J.If i = super.visitIf(iff, s);
            if (i.getThenPart() instanceof J.Block) {
                brace(i.getThenPart().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
                if (i.getElsePart() != null) {
                    keyword(i.getElsePart().getPrefix(), s.elseOnNewLine, s);
                }
            }
            return i;
        }

        @Override
        public J.If.Else visitElse(J.If.Else anElse, Statistics s) {
            J.If.Else e = super.visitElse(anElse, s);
            if (e.getBody() instanceof J.Block) {
                brace(e.getBody().getPrefix(), s.beforeKeywordBrace, s.statementBraceOnNewLine);
            }
            return e;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, Statistics s) {
            J.WhileLoop w = super.visitWhileLoop(whileLoop, s);
            if (w.getBody() instanceof J.Block) {
                brace(w.getBody().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            }
            return w;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, Statistics s) {
            J.ForLoop f = super.visitForLoop(forLoop, s);
            if (f.getBody() instanceof J.Block) {
                brace(f.getBody().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            }
            return f;
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, Statistics s) {
            J.ForEachLoop f = super.visitForEachLoop(forLoop, s);
            if (f.getBody() instanceof J.Block) {
                brace(f.getBody().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            }
            return f;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, Statistics s) {
            J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, s);
            if (d.getBody() instanceof J.Block) {
                brace(d.getBody().getPrefix(), s.beforeKeywordBrace, s.statementBraceOnNewLine);
                keyword(d.getPadding().getWhileCondition().getBefore(), s.whileOnNewLine, s);
            }
            return d;
        }

        @Override
        public J.Switch visitSwitch(J.Switch aSwitch, Statistics s) {
            J.Switch sw = super.visitSwitch(aSwitch, s);
            brace(sw.getCases().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            return sw;
        }

        @Override
        public J.Try visitTry(J.Try tryable, Statistics s) {
            J.Try t = super.visitTry(tryable, s);
            brace(t.getBody().getPrefix(), s.beforeKeywordBrace, s.statementBraceOnNewLine);
            for (J.Try.Catch c : t.getCatches()) {
                keyword(c.getPrefix(), s.catchOnNewLine, s);
                brace(c.getBody().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            }
            JLeftPadded<J.Block> finallyBlock = t.getPadding().getFinally();
            if (finallyBlock != null) {
                keyword(finallyBlock.getBefore(), s.catchOnNewLine, s);
                brace(finallyBlock.getElement().getPrefix(), s.beforeKeywordBrace, s.statementBraceOnNewLine);
            }
            return t;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Statistics s) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, s);
            if (TemplateOutput.isTagSyntax(c)) {
                tagSyntax(c.getMarkers(), s);
            } else {
                brace(c.getBody().getPrefix(), s.beforeComponentBrace, s.componentBraceOnNewLine);
            }
            return c;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Statistics s) {
            J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, s);
            if (TemplateOutput.isTagSyntax(v)) {
                tagSyntax(v.getMarkers(), s);
            } else if (v.getTypeExpression() != null && !v.getVariables().isEmpty() &&
                       getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                // a parameter, whose name an author may line up with those of the parameters around it
                aligned(s.alignedAssignments, v.getVariables().get(0).getPrefix());
            }
            return v;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, Statistics s) {
            J.Annotation a = super.visitAnnotation(annotation, s);
            aligned(s.alignedAttributes, a.getPrefix());
            JContainer<Expression> arguments = a.getPadding().getArguments();
            if (arguments != null && !arguments.getElements().isEmpty()) {
                Count equals = inTagSyntax(getCursor()) ? s.attributeEquals : s.metadataEquals;
                count(equals, arguments.getBefore());
                count(equals, arguments.getElements().get(0).getPrefix());
            }
            return a;
        }

        @Override
        public Cf.TagAttribute visitTagAttribute(Cf.TagAttribute attribute, Statistics s) {
            Cf.TagAttribute a = super.visitTagAttribute(attribute, s);
            aligned(s.alignedAttributes, a.getPrefix());
            JLeftPadded<Expression> value = a.getPadding().getValue();
            if (value != null) {
                Count equals = getCursor().getParentTreeCursor().getValue() instanceof Cf.Tag ? s.attributeEquals : s.metadataEquals;
                count(equals, value.getBefore());
                count(equals, value.getElement().getPrefix());
            }
            return a;
        }

        @Override
        public Cf.Tag visitTag(Cf.Tag tag, Statistics s) {
            Cf.Tag t = super.visitTag(tag, s);
            count(t.isSelfClosing() ? s.beforeSelfClosingEnd : s.beforeTagEnd, t.getBeforeEnd());
            if (t.getClosing() != null) {
                count(s.beforeTagEnd, t.getClosing().getBeforeEnd());
            }
            return t;
        }

        @Override
        public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, Statistics s) {
            Cf.ScriptTag t = super.visitScriptTag(scriptTag, s);
            if (t.getBody() != null) {
                brace(t.getBody().getPrefix(), s.beforeComponentBrace, s.statementBraceOnNewLine);
            }
            return t;
        }

        @Override
        public Cf.TagCall visitTagCall(Cf.TagCall tagCall, Statistics s) {
            Cf.TagCall t = super.visitTagCall(tagCall, s);
            brace(t.getBody().getPrefix(), s.beforeStatementBrace, s.statementBraceOnNewLine);
            return t;
        }

        private static void tagSyntax(Markers markers, Statistics s) {
            markers.findFirst(TagSyntax.class).ifPresent(tag -> {
                count(tag.isSelfClosing() ? s.beforeSelfClosingEnd : s.beforeTagEnd, tag.getBeforeEnd());
                if (tag.getClosing() != null) {
                    count(s.beforeTagEnd, tag.getClosing().getBeforeEnd());
                }
            });
        }

        /**
         * Whether an annotation belongs to a declaration written as a tag, whose attributes are a tag's.
         */
        private static boolean inTagSyntax(Cursor cursor) {
            Object owner = cursor.getParentTreeCursor().getValue();
            return owner instanceof J && TemplateOutput.isTagSyntax((J) owner);
        }

        private static <T extends J> void delimited(List<JRightPadded<T>> elements, Count within, Count emptyWithin, Statistics s) {
            if (elements.isEmpty()) {
                return;
            }
            if (elements.size() == 1 && elements.get(0).getElement() instanceof J.Empty) {
                count(emptyWithin, elements.get(0).getElement().getPrefix());
                return;
            }
            count(within, elements.get(0).getElement().getPrefix());
            count(within, elements.get(elements.size() - 1).getAfter());
            Space first = elements.get(0).getElement().getPrefix();
            Space closer = elements.get(elements.size() - 1).getAfter();
            if (Layout.hasLineBreak(first) && Layout.hasLineBreak(closer)) {
                s.closingBracketsIndented.add(width(indent(closer)) >= width(indent(first)));
            }
            for (int i = 0; i < elements.size() - 1; i++) {
                if (!elements.get(i).getMarkers().findFirst(OmitComma.class).isPresent()) {
                    count(s.beforeComma, elements.get(i).getAfter());
                    count(s.afterComma, elements.get(i + 1).getElement().getPrefix());
                }
            }
        }

        private static void brace(Space prefix, @Nullable Count spaced, Count onNewLine) {
            if (!prefix.getComments().isEmpty()) {
                return;
            }
            boolean lineBreak = Layout.hasLineBreak(prefix.getWhitespace());
            onNewLine.add(lineBreak);
            if (!lineBreak && spaced != null) {
                spaced.add(!prefix.getWhitespace().isEmpty());
            }
        }

        /**
         * Counts whether a space before an operator or attribute lines it up, with more than one space.
         */
        private static void aligned(Count count, Space space) {
            String whitespace = space.getWhitespace();
            if (space.getComments().isEmpty() && !whitespace.isEmpty() && !Layout.hasLineBreak(whitespace)) {
                count.add(whitespace.length() > 1);
            }
        }

        private static void keyword(Space prefix, Count onNewLine, Statistics s) {
            if (!prefix.getComments().isEmpty()) {
                return;
            }
            boolean lineBreak = Layout.hasLineBreak(prefix.getWhitespace());
            onNewLine.add(lineBreak);
            if (!lineBreak) {
                s.beforeKeywordAfterBlock.add(!prefix.getWhitespace().isEmpty());
            }
        }

        /**
         * Counts a space on one line, which has a space in it or not. A space with a line break or a comment says
         * nothing about the spaces of a line.
         */
        private static void count(Count count, Space space) {
            if (space.getComments().isEmpty() && !Layout.hasLineBreak(space.getWhitespace())) {
                count.add(!space.getWhitespace().isEmpty());
            }
        }

        private static void emptyLines(Space space, Statistics s) {
            emptyLines(space.getWhitespace(), s);
            for (Comment comment : space.getComments()) {
                emptyLines(comment.getSuffix(), s);
            }
        }

        /**
         * Counts the blank lines whitespace holds, which have spaces or tabs in them or not.
         */
        private static void emptyLines(String whitespace, Statistics s) {
            int start = -1;
            for (int i = 0; i < whitespace.length(); i++) {
                char c = whitespace.charAt(i);
                if (Layout.isLineBreak(c)) {
                    if (start >= 0) {
                        s.indentedEmptyLines.add(i > start);
                    }
                    if (c == '\r' && i + 1 < whitespace.length() && whitespace.charAt(i + 1) == '\n') {
                        i++;
                    }
                    start = i + 1;
                }
            }
        }

        private static void blankLines(Space space, Histogram histogram) {
            if (Layout.hasLineBreak(space.getWhitespace())) {
                histogram.add(Layout.lineBreaks(space.getWhitespace()) - 1);
            }
            for (Comment comment : space.getComments()) {
                if (Layout.hasLineBreak(comment.getSuffix())) {
                    histogram.add(Layout.lineBreaks(comment.getSuffix()) - 1);
                }
            }
        }

        /**
         * The blank lines before a declaration and the comments on lines of their own that belong to it.
         */
        private static int blankLinesBefore(Space prefix) {
            if (prefix.getComments().isEmpty() || Layout.hasLineBreak(prefix.getWhitespace())) {
                return Math.max(0, Layout.lineBreaks(prefix.getWhitespace()) - 1);
            }
            return Math.max(0, Layout.lineBreaks(prefix.getComments().get(0).getSuffix()) - 1);
        }

        private static String lineOf(Cursor cursor) {
            String indent = cursor.getNearestMessage(INDENT);
            return indent == null ? "" : indent;
        }

        private static String indent(Space prefix) {
            String segment = prefix.getWhitespace();
            for (Comment comment : prefix.getComments()) {
                if (Layout.hasLineBreak(comment.getSuffix())) {
                    segment = comment.getSuffix();
                }
            }
            return Layout.lastLine(segment);
        }

        private static int width(String indent) {
            return Layout.width(indent, 4);
        }
    }
}
