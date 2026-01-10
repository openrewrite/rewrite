/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.python.style;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.python.tree.Py;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.max;

public class Autodetect extends NamedStyles implements PythonStyle {
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.python.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(),
                styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static class Detector {
        private final IndentStatistics indentStatistics = new IndentStatistics();
        private final FindIndentVisitor indentVisitor = new FindIndentVisitor();
        private final SpacesStatistics spacesStatistics = new SpacesStatistics();
        private final FindSpaceVisitor spacesVisitor = new FindSpaceVisitor();

        public void sample(SourceFile python) {
            if (python instanceof Py.CompilationUnit) {
                indentVisitor.visitNonNull(python, indentStatistics);
                spacesVisitor.visitNonNull(python, spacesStatistics);
            }
        }

        public Autodetect build() {
            return new Autodetect(
                    Tree.randomId(),
                    Arrays.asList(indentStatistics.getTabsAndIndentsStyle(), spacesStatistics.getStyle())
            );
        }
    }

    private static class FindIndentVisitor extends PythonIsoVisitor<IndentStatistics> {
        private int currentBlockIndent = 0;

        private int countSpaces(String s) {
            int withoutSpaces = s.replaceAll(" ", "").length();
            return s.length() - withoutSpaces;
        }

        @Override
        public J.Block visitBlock(J.Block block, IndentStatistics indentStatistics) {
            int blockIndentBeforeThisBlock = currentBlockIndent;
            int currentBlockIndentSize = 0;

            if (!block.getStatements().isEmpty()) {
                this.currentBlockIndent = countSpaces(block.getStatements().get(0).getPrefix().getWhitespace());
                currentBlockIndentSize = this.currentBlockIndent - blockIndentBeforeThisBlock;
            }

            for (Statement s : block.getStatements()) {
                int spaceCount = countSpaces(s.getPrefix().getWhitespace());
                int relativeIndentSize = spaceCount - this.currentBlockIndent;
                int classifyAsIndentSize = relativeIndentSize + currentBlockIndentSize;
                indentStatistics.countsByIndentSize.put(classifyAsIndentSize, indentStatistics.countsByIndentSize.getOrDefault(classifyAsIndentSize, 0) + 1);
            }
            J.Block ret = super.visitBlock(block, indentStatistics);
            this.currentBlockIndent = blockIndentBeforeThisBlock;
            return ret;
        }
    }

    private static class IndentStatistics {
        private final Map<Integer, Integer> countsByIndentSize = new HashMap<>();

        private <T> T keyWithHighestCount(Map<T, Integer> counts) {
            int max = max(counts.values());
            return counts.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == max)
                    .findFirst().get().getKey();
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            if (countsByIndentSize.isEmpty()) {
                return IntelliJ.tabsAndIndents();
            }
            return IntelliJ.tabsAndIndents().withIndentSize(keyWithHighestCount(countsByIndentSize));
        }
    }

    private static class SpacesStatistics {
        int beforeParenthesesMethodCall = 0;
        int beforeParenthesesMethodDeclaration = 0;
        int beforeParenthesesLeftBracket = 0;

        int aroundOperatorsAssignment = 0;
        int aroundOperatorsEquality = 0;
        int aroundOperatorsRelational = 0;
        int aroundOperatorsBitwise = 0;
        int aroundOperatorsAdditive = 0;
        int aroundOperatorsMultiplicative = 0;
        int aroundOperatorsShift = 0;
        int aroundOperatorsPower = 0;
        int aroundOperatorsEqInNamedParameter = 0;
        int aroundOperatorsEqInKeywordArgument = 0;

        int withinBrackets = 0;
        int withinMethodDeclarationParentheses = 0;
        int withinEmptyMethodDeclarationParentheses = 0;
        int withinMethodCallParentheses = 0;
        int withinEmptyMethodCallParentheses = 0;
        int withinBraces = 0;

        int otherBeforeComma = 0;
        int otherAfterComma = 0;
        int otherBeforeForSemicolon = 0;
        int otherBeforeColon = 0;
        int otherAfterColon = 0;
        int otherBeforeBackslash = 0;
        int otherBeforeHash = 0;
        int otherAfterHash = 0;

        // Helper method to determine boolean value based on count
        private static Boolean determineStyleValue(int count, Boolean defaultValue) {
            if (count > 0) return true;
            if (count < 0) return false;
            return defaultValue;
        }

        public SpacesStyle getStyle() {
            SpacesStyle defaults = IntelliJ.spaces();
            return defaults
                    .withBeforeParentheses(defaults.getBeforeParentheses()
                            .withMethodCall(determineStyleValue(beforeParenthesesMethodCall, defaults.getBeforeParentheses().getMethodCall()))
                            .withMethodDeclaration(determineStyleValue(beforeParenthesesMethodDeclaration, defaults.getBeforeParentheses().getMethodDeclaration()))
                            .withLeftBracket(determineStyleValue(beforeParenthesesLeftBracket, defaults.getBeforeParentheses().getLeftBracket())))
                    .withAroundOperators(defaults.getAroundOperators()
                            .withAssignment(determineStyleValue(aroundOperatorsAssignment, defaults.getAroundOperators().getAssignment()))
                            .withEquality(determineStyleValue(aroundOperatorsEquality, defaults.getAroundOperators().getEquality()))
                            .withRelational(determineStyleValue(aroundOperatorsRelational, defaults.getAroundOperators().getRelational()))
                            .withBitwise(determineStyleValue(aroundOperatorsBitwise, defaults.getAroundOperators().getBitwise()))
                            .withAdditive(determineStyleValue(aroundOperatorsAdditive, defaults.getAroundOperators().getAdditive()))
                            .withMultiplicative(determineStyleValue(aroundOperatorsMultiplicative, defaults.getAroundOperators().getMultiplicative()))
                            .withShift(determineStyleValue(aroundOperatorsShift, defaults.getAroundOperators().getShift()))
                            .withPower(determineStyleValue(aroundOperatorsPower, defaults.getAroundOperators().getPower()))
                            .withEqInNamedParameter(determineStyleValue(aroundOperatorsEqInNamedParameter, defaults.getAroundOperators().getEqInNamedParameter()))
                            .withEqInKeywordArgument(determineStyleValue(aroundOperatorsEqInKeywordArgument, defaults.getAroundOperators().getEqInKeywordArgument())))
                    .withWithin(defaults.getWithin()
                            .withBrackets(determineStyleValue(withinBrackets, defaults.getWithin().getBrackets()))
                            .withMethodDeclarationParentheses(determineStyleValue(withinMethodDeclarationParentheses, defaults.getWithin().getMethodDeclarationParentheses()))
                            .withEmptyMethodDeclarationParentheses(determineStyleValue(withinEmptyMethodDeclarationParentheses, defaults.getWithin().getEmptyMethodDeclarationParentheses()))
                            .withMethodCallParentheses(determineStyleValue(withinMethodCallParentheses, defaults.getWithin().getMethodCallParentheses()))
                            .withEmptyMethodCallParentheses(determineStyleValue(withinEmptyMethodCallParentheses, defaults.getWithin().getEmptyMethodCallParentheses()))
                            .withBraces(determineStyleValue(withinBraces, defaults.getWithin().getBraces())))
                    .withOther(defaults.getOther()
                            .withBeforeComma(determineStyleValue(otherBeforeComma, defaults.getOther().getBeforeComma()))
                            .withAfterComma(determineStyleValue(otherAfterComma, defaults.getOther().getAfterComma()))
                            .withBeforeForSemicolon(determineStyleValue(otherBeforeForSemicolon, defaults.getOther().getBeforeForSemicolon()))
                            .withBeforeColon(determineStyleValue(otherBeforeColon, defaults.getOther().getBeforeColon()))
                            .withAfterColon(determineStyleValue(otherAfterColon, defaults.getOther().getAfterColon()))
                            .withBeforeBackslash(determineStyleValue(otherBeforeBackslash, defaults.getOther().getBeforeBackslash()))
                            .withBeforeHash(determineStyleValue(otherBeforeHash, defaults.getOther().getBeforeHash()))
                            .withAfterHash(determineStyleValue(otherAfterHash, defaults.getOther().getAfterHash())));
        }
    }

    private static class FindSpaceVisitor extends PythonIsoVisitor<SpacesStatistics> {

        private static boolean variableIsPartOfMethodHeader(Cursor cursor) {
            return cursor.getParentTreeCursor().getValue() instanceof J.VariableDeclarations &&
                   cursor.getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration;
        }

        private static int hasSpace(@Nullable Space space) {
            if (space == null) {
                return 0;
            }
            return space.getWhitespace().contains(" ") ? 1 : -1;
        }

        private static int hasSpaceBefore(@Nullable JContainer<?> container) {
            if (container != null) {
                return hasSpace(container.getBefore());
            }
            return 0;
        }

        private static int hasSpaceBefore(@Nullable JLeftPadded<?> container) {
            if (container != null) {
                return hasSpace(container.getBefore());
            }
            return 0;
        }

        private static <T extends J> int hasSpaceBeforeElement(@Nullable JRightPadded<T> padded) {
            if (padded != null) {
                return hasSpace(padded.getElement().getPrefix());
            }
            return 0;
        }

        private static <T extends J> int hasSpaceBeforeElement(@Nullable JLeftPadded<T> padded) {
            if (padded != null) {
                return hasSpace(padded.getElement().getPrefix());
            }
            return 0;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, SpacesStatistics statistics) {
            try {
                super.visit(tree, statistics);
            } catch (Exception e) {
                // Suppress errors. A malformed element should not fail parsing overall.
            }
            return (J) tree;
        }

        @Override
        public J.Block visitBlock(J.Block block, SpacesStatistics statistics) {
            statistics.otherBeforeColon += hasSpace(block.getPrefix());
            return super.visitBlock(block, statistics);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable v, SpacesStatistics statistics) {
            v = super.visitVariable(v, statistics);
            if (variableIsPartOfMethodHeader(getCursor())) {
                return v;
            }

            J.VariableDeclarations parent = getCursor().dropParentUntil(J.VariableDeclarations.class::isInstance).getValue();
            if (v.getPadding().getInitializer() != null && parent.getTypeExpression() == null) {
                statistics.aroundOperatorsEqInNamedParameter += hasSpaceBefore(v.getPadding().getInitializer());
                statistics.aroundOperatorsEqInNamedParameter += hasSpaceBeforeElement(v.getPadding().getInitializer());
            }
            return v;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, SpacesStatistics statistics) {
            statistics.beforeParenthesesMethodCall += hasSpaceBefore(classDecl.getPadding().getImplements());
            if (classDecl.getImplements() != null) {
                if (!classDecl.getImplements().isEmpty()) {
                    statistics.withinMethodCallParentheses +=
                            hasSpace(classDecl.getImplements().get(0).getPrefix());
                }
            }

            return super.visitClassDeclaration(classDecl, statistics);
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, SpacesStatistics statistics) {
            J.Binary b = super.visitBinary(binary, statistics);
            J.Binary.Type op = b.getOperator();

            // Space at either side will be accepted
            int hasSpaceAround = Integer.max(hasSpace(b.getPadding().getOperator().getBefore()),
                    hasSpace(b.getRight().getPrefix()));

            if (op == J.Binary.Type.Addition || op == J.Binary.Type.Subtraction) {
                statistics.aroundOperatorsAdditive += hasSpaceAround;
            } else if (op == J.Binary.Type.Multiplication || op == J.Binary.Type.Division || op == J.Binary.Type.Modulo) {
                statistics.aroundOperatorsMultiplicative += hasSpaceAround;
            } else if (op == J.Binary.Type.Equal || op == J.Binary.Type.NotEqual) {
                statistics.aroundOperatorsEquality += hasSpaceAround;
            } else if (op == J.Binary.Type.LessThan || op == J.Binary.Type.GreaterThan || op == J.Binary.Type.LessThanOrEqual || op == J.Binary.Type.GreaterThanOrEqual) {
                statistics.aroundOperatorsRelational += hasSpaceAround;
            } else if (op == J.Binary.Type.BitAnd || op == J.Binary.Type.BitOr || op == J.Binary.Type.BitXor) {
                statistics.aroundOperatorsBitwise += hasSpaceAround;
            } else if (op == J.Binary.Type.LeftShift || op == J.Binary.Type.RightShift || op == J.Binary.Type.UnsignedRightShift) {
                statistics.aroundOperatorsShift += hasSpaceAround;
            }
            return b;
        }

        @Override
        public Py.Binary visitBinary(Py.Binary binary, SpacesStatistics statistics) {
            Py.Binary b = super.visitBinary(binary, statistics);
            Py.Binary.Type op = b.getOperator();

            // Space at either side will be accepted
            int hasSpaceAround = Integer.max(hasSpace(b.getPadding().getOperator().getBefore()),
                    hasSpace(b.getRight().getPrefix()));

            if (op == Py.Binary.Type.FloorDivision || op == Py.Binary.Type.MatrixMultiplication) {
                statistics.aroundOperatorsMultiplicative += hasSpaceAround;
            } else if (op == Py.Binary.Type.StringConcatenation) {
                statistics.aroundOperatorsAdditive += hasSpaceAround;
            } else if (op == Py.Binary.Type.Power) {
                statistics.aroundOperatorsPower += hasSpaceAround;
            }

            return b;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, SpacesStatistics statistics) {
            statistics.beforeParenthesesMethodDeclaration +=
                    hasSpace(method.getPadding().getParameters().getBefore());

            if (!method.getParameters().isEmpty()) {
                statistics.withinMethodDeclarationParentheses +=
                        hasSpace(method.getParameters().get(0).getPrefix());
            } else {
                statistics.withinEmptyMethodDeclarationParentheses +=
                        hasSpace(method.getPadding().getParameters().getBefore());
            }

            return super.visitMethodDeclaration(method, statistics);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, SpacesStatistics statistics) {
            statistics.beforeParenthesesMethodCall +=
                    hasSpace(method.getPadding().getArguments().getBefore());

            if (!method.getArguments().isEmpty()) {
                statistics.withinMethodCallParentheses +=
                        hasSpace(method.getArguments().get(0).getPrefix());
            } else {
                statistics.withinEmptyMethodCallParentheses +=
                        hasSpace(method.getPadding().getArguments().getBefore());
            }

            return super.visitMethodInvocation(method, statistics);
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, SpacesStatistics statistics) {
            statistics.aroundOperatorsAssignment +=
                    hasSpace(assignment.getPadding().getAssignment().getBefore());
            statistics.aroundOperatorsAssignment +=
                    hasSpace(assignment.getAssignment().getPrefix());

            return super.visitAssignment(assignment, statistics);
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, SpacesStatistics statistics) {
            statistics.aroundOperatorsAssignment +=
                    hasSpace(assignOp.getPadding().getOperator().getBefore());
            statistics.aroundOperatorsAssignment +=
                    hasSpace(assignOp.getAssignment().getPrefix());
            return super.visitAssignmentOperation(assignOp, statistics);
        }

        @Override
        public Py.ChainedAssignment visitChainedAssignment(Py.ChainedAssignment chainedAssignment, SpacesStatistics statistics) {
            for (JRightPadded<Expression> variable : chainedAssignment.getPadding().getVariables()) {
                statistics.aroundOperatorsAssignment += hasSpace(variable.getAfter());
            }
            statistics.aroundOperatorsAssignment +=
                    hasSpace(chainedAssignment.getAssignment().getPrefix());

            return super.visitChainedAssignment(chainedAssignment, statistics);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, SpacesStatistics statistics) {
            if (memberRef.getPadding().getTypeParameters() != null) {
                statistics.withinBrackets +=
                        hasSpace(memberRef.getPadding().getTypeParameters().getBefore());
            }

            return super.visitMemberReference(memberRef, statistics);
        }

        @Override
        public J.If visitIf(J.If iff, SpacesStatistics statistics) {
            statistics.otherBeforeColon += hasSpace(iff.getPadding().getThenPart().getAfter());
            return super.visitIf(iff, statistics);
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, SpacesStatistics statistics) {
            JContainer<Expression> typeParams = type.getPadding().getTypeParameters();
            if (typeParams != null) {
                statistics.withinBrackets +=
                        hasSpace(typeParams.getBefore());

                if (!typeParams.getElements().isEmpty()) {
                    statistics.withinBrackets +=
                            hasSpace(typeParams.getElements().get(0).getPrefix());
                }
            }

            return super.visitParameterizedType(type, statistics);
        }

        @Override
        public Py.CollectionLiteral visitCollectionLiteral(Py.CollectionLiteral coll, SpacesStatistics statistics) {
            List<Expression> el = coll.getElements();
            if (coll.getKind() == Py.CollectionLiteral.Kind.LIST) {
                if (!el.isEmpty()) {
                    statistics.withinBrackets += hasSpace(el.get(0).getPrefix());
                }
            } else if ((coll.getKind() == Py.CollectionLiteral.Kind.SET || coll.getKind() == Py.CollectionLiteral.Kind.TUPLE) && !el.isEmpty()) {
                    statistics.withinBraces += hasSpace(el.get(0).getPrefix());
                }

            return super.visitCollectionLiteral(coll, statistics);
        }

        @Override
        public Py.TypeHint visitTypeHint(Py.TypeHint typeHint, SpacesStatistics statistics) {
            statistics.otherBeforeColon += hasSpace(typeHint.getPrefix());
            statistics.otherAfterColon += hasSpace(typeHint.getTypeTree().getPrefix());

            return super.visitTypeHint(typeHint, statistics);
        }

        @Override
        public Py.ComprehensionExpression visitComprehensionExpression(Py.ComprehensionExpression comp, SpacesStatistics statistics) {
            if (comp.getKind() == Py.ComprehensionExpression.Kind.LIST) {
                statistics.withinBrackets += hasSpace(comp.getResult().getPrefix());
            } else if (comp.getKind() == Py.ComprehensionExpression.Kind.SET || comp.getKind() == Py.ComprehensionExpression.Kind.DICT) {
                statistics.withinBraces += hasSpace(comp.getResult().getPrefix());
            }
            return super.visitComprehensionExpression(comp, statistics);
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, SpacesStatistics statistics) {
            statistics.beforeParenthesesLeftBracket +=
                    hasSpace(arrayAccess.getDimension().getPrefix());

            if (arrayAccess.getDimension().getPadding().getIndex() != null) {
                statistics.withinBrackets +=
                        hasSpace(arrayAccess.getDimension().getPadding().getIndex().getElement().getPrefix());
            }

            return super.visitArrayAccess(arrayAccess, statistics);
        }

        @Override
        public Py.NamedArgument visitNamedArgument(Py.NamedArgument namedArg, SpacesStatistics statistics) {
            if (namedArg.getPadding().getValue() != null) {
                statistics.aroundOperatorsEqInKeywordArgument +=
                        hasSpace(namedArg.getPadding().getValue().getBefore());
                statistics.aroundOperatorsEqInKeywordArgument +=
                        hasSpace(namedArg.getPadding().getValue().getElement().getPrefix());
            }

            return super.visitNamedArgument(namedArg, statistics);
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, SpacesStatistics statistics) {
            // Check space before colon (which is in the body's Block prefix)
            if (forLoop.getBody() instanceof J.Block) {
                statistics.otherBeforeColon += hasSpace(forLoop.getBody().getPrefix());
            }
            return super.visitForEachLoop(forLoop, statistics);
        }

        @Override
        public Py.Slice visitSlice(Py.Slice slice, SpacesStatistics statistics) {
            // Check spaces within slice brackets
            statistics.withinBrackets += hasSpaceBeforeElement(slice.getPadding().getStart());
            statistics.withinBrackets += hasSpaceBeforeElement(slice.getPadding().getStop());
            statistics.withinBrackets += hasSpaceBeforeElement(slice.getPadding().getStep());
            return super.visitSlice(slice, statistics);
        }

        @Override
        public Py.DictLiteral visitDictLiteral(Py.DictLiteral dict, SpacesStatistics statistics) {
            if (!dict.getElements().isEmpty()) {
                int size = dict.getPadding().getElements().getPadding().getElements().size();
                for (int i = 0; i < size; i++) {
                    JRightPadded<Expression> exp = dict.getPadding().getElements().getPadding().getElements().get(i);
                    if (i == 0) {
                        statistics.withinBraces += hasSpaceBeforeElement(exp);
                    } else if (i == (size - 1)) {
                        statistics.withinBraces += hasSpace(exp.getAfter());
                    } else {
                        statistics.otherBeforeComma += hasSpace(exp.getAfter());
                        statistics.otherAfterComma += hasSpaceBeforeElement(exp);
                    }
                }
            }
            return super.visitDictLiteral(dict, statistics);
        }
    }
}
