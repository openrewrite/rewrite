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
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.marker.*;
import org.openrewrite.python.tree.Py;

import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Prints Python LSTs. The printer in {@code rewrite/python/printer.py} is its counterpart in the
 * Python process, and the two must produce identical output for the same tree.
 */
public class PythonPrinter<P> extends PythonVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private final PythonJavaPrinter delegate = new PythonJavaPrinter();

    @Override
    public @Nullable J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof Py)) {
            // a cursor passed to visit(tree, p, parent) has only been set on this printer
            delegate.setCursor(getCursor());
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        }
        return super.visit(tree, p);
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        delegate.setCursor(cursor);
    }

    @Override
    public J visitCompilationUnit(Py.CompilationUnit cu, PrintOutputCapture<P> p) {
        if (cu.isCharsetBomMarked()) {
            p.append('\uFEFF');
        }
        beforeSyntax(cu, p);
        for (JRightPadded<J.Import> anImport : cu.getPadding().getImports()) {
            visitRightPadded(anImport, p);
        }
        for (JRightPadded<Statement> statement : cu.getPadding().getStatements()) {
            visitRightPadded(statement, p);
        }
        visitSpace(cu.getEof(), p);
        if (cu.getMarkers().findFirst(SuppressNewline.class).isPresent()) {
            int length = p.out.length();
            if (length > 0 && p.out.charAt(length - 1) == '\n') {
                p.out.setLength(length - 1);
            }
        }
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitAsync(Py.Async async, PrintOutputCapture<P> p) {
        beforeSyntax(async, p);
        p.append("async");
        visit(async.getStatement(), p);
        return async;
    }

    @Override
    public J visitShebang(Py.Shebang shebang, PrintOutputCapture<P> p) {
        beforeSyntax(shebang, p);
        p.append(shebang.getText());
        afterSyntax(shebang, p);
        return shebang;
    }

    @Override
    public J visitAwait(Py.Await await, PrintOutputCapture<P> p) {
        beforeSyntax(await, p);
        p.append("await");
        visit(await.getExpression(), p);
        return await;
    }

    @Override
    public J visitBinary(Py.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        switch (binary.getOperator()) {
            case NotIn:
                p.append("not");
                if (binary.getNegation() != null) {
                    visitSpace(binary.getNegation(), p);
                } else {
                    p.append(' ');
                }
                p.append("in");
                break;
            case In:
                p.append("in");
                break;
            case Is:
                p.append("is");
                break;
            case IsNot:
                p.append("is");
                if (binary.getNegation() != null) {
                    visitSpace(binary.getNegation(), p);
                } else {
                    p.append(' ');
                }
                p.append("not");
                break;
            case FloorDivision:
                p.append("//");
                break;
            case MatrixMultiplication:
                p.append("@");
                break;
            case Power:
                p.append("**");
                break;
            case StringConcatenation:
                break;
        }
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitChainedAssignment(Py.ChainedAssignment chainedAssignment, PrintOutputCapture<P> p) {
        beforeSyntax(chainedAssignment, p);
        visitRightPadded(chainedAssignment.getPadding().getVariables(), "=", p);
        p.append('=');
        visit(chainedAssignment.getAssignment(), p);
        afterSyntax(chainedAssignment, p);
        return chainedAssignment;
    }

    @Override
    public J visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(collectionLiteral, p);
        JContainer<Expression> elements = collectionLiteral.getPadding().getElements();
        switch (collectionLiteral.getKind()) {
            case LIST:
                visitContainer("[", elements, ",", "]", p);
                break;
            case SET:
                visitContainer("{", elements, ",", "}", p);
                break;
            case TUPLE:
                if (elements.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    visitContainer("", elements, ",", "", p);
                } else {
                    visitContainer("(", elements, ",", ")", p);
                }
                break;
        }
        afterSyntax(collectionLiteral, p);
        return collectionLiteral;
    }

    @Override
    public J visitComprehensionExpression(Py.ComprehensionExpression comprehension, PrintOutputCapture<P> p) {
        beforeSyntax(comprehension, p);
        String open;
        String close;
        switch (comprehension.getKind()) {
            case DICT:
            case SET:
                open = "{";
                close = "}";
                break;
            case LIST:
                open = "[";
                close = "]";
                break;
            case GENERATOR:
                if (comprehension.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    open = "";
                    close = "";
                } else {
                    open = "(";
                    close = ")";
                }
                break;
            default:
                throw new IllegalStateException("Unknown comprehension kind: " + comprehension.getKind());
        }
        p.append(open);
        visit(comprehension.getResult(), p);
        for (Py.ComprehensionExpression.Clause clause : comprehension.getClauses()) {
            visit(clause, p);
        }
        visitSpace(comprehension.getSuffix(), p);
        p.append(close);
        afterSyntax(comprehension, p);
        return comprehension;
    }

    @Override
    public J visitComprehensionClause(Py.ComprehensionExpression.Clause clause, PrintOutputCapture<P> p) {
        beforeSyntax(clause, p);
        JRightPadded<Boolean> async = clause.getPadding().getAsync();
        if (async != null && async.getElement()) {
            p.append("async");
            visitSpace(async.getAfter(), p);
        }
        p.append("for");
        visit(clause.getIteratorVariable(), p);
        visitSpace(clause.getPadding().getIteratedList().getBefore(), p);
        p.append("in");
        visit(clause.getIteratedList(), p);
        if (clause.getConditions() != null) {
            for (Py.ComprehensionExpression.Condition condition : clause.getConditions()) {
                visit(condition, p);
            }
        }
        return clause;
    }

    @Override
    public J visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, PrintOutputCapture<P> p) {
        beforeSyntax(condition, p);
        p.append("if");
        visit(condition.getExpression(), p);
        return condition;
    }

    @Override
    public J visitDel(Py.Del del, PrintOutputCapture<P> p) {
        beforeSyntax(del, p);
        p.append("del");
        visitRightPadded(del.getPadding().getTargets(), ",", p);
        return del;
    }

    @Override
    public J visitDictLiteral(Py.DictLiteral dictLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(dictLiteral, p);
        visitContainer("{", dictLiteral.getPadding().getElements(), ",", "}", p);
        afterSyntax(dictLiteral, p);
        return dictLiteral;
    }

    @Override
    public J visitErrorFrom(Py.ErrorFrom errorFrom, PrintOutputCapture<P> p) {
        beforeSyntax(errorFrom, p);
        visit(errorFrom.getError(), p);
        visitSpace(errorFrom.getPadding().getFrom().getBefore(), p);
        p.append("from");
        visit(errorFrom.getFrom(), p);
        return errorFrom;
    }

    @Override
    public J visitExceptionType(Py.ExceptionType exceptionType, PrintOutputCapture<P> p) {
        beforeSyntax(exceptionType, p);
        if (exceptionType.isExceptionGroup()) {
            p.append("*");
        }
        visit(exceptionType.getExpression(), p);
        return exceptionType;
    }

    @Override
    public J visitExpressionStatement(Py.ExpressionStatement expressionStatement, PrintOutputCapture<P> p) {
        visit(expressionStatement.getExpression(), p);
        return expressionStatement;
    }

    @Override
    public J visitExpressionTypeTree(Py.ExpressionTypeTree expressionTypeTree, PrintOutputCapture<P> p) {
        beforeSyntax(expressionTypeTree, p);
        visit(expressionTypeTree.getReference(), p);
        afterSyntax(expressionTypeTree, p);
        return expressionTypeTree;
    }

    @Override
    public J visitFormattedString(Py.FormattedString formattedString, PrintOutputCapture<P> p) {
        beforeSyntax(formattedString, p);
        String delimiter = formattedString.getDelimiter();
        p.append(delimiter);
        for (Expression part : formattedString.getParts()) {
            visit(part, p);
        }
        //noinspection ConstantValue
        if (delimiter != null) {
            // the closing delimiter is the opening one without its string prefix
            int quote = Math.max(delimiter.indexOf('\''), delimiter.indexOf('"'));
            if (quote >= 0) {
                p.append(delimiter.substring(quote));
            }
        }
        return formattedString;
    }

    @Override
    public J visitFormattedStringValue(Py.FormattedString.Value value, PrintOutputCapture<P> p) {
        beforeSyntax(value, p);
        p.append('{');
        visitRightPadded(value.getPadding().getExpression(), p);
        JRightPadded<Boolean> debug = value.getPadding().getDebug();
        if (debug != null) {
            p.append('=');
            visitSpace(debug.getAfter(), p);
        }
        JRightPadded<Py.FormattedString.Value.Conversion> conversion = value.getPadding().getConversion();
        if (conversion != null) {
            p.append('!');
            switch (conversion.getElement()) {
                case STR:
                    p.append('s');
                    break;
                case REPR:
                    p.append('r');
                    break;
                case ASCII:
                    p.append('a');
                    break;
            }
            visitSpace(conversion.getAfter(), p);
        }
        if (value.getFormat() != null) {
            p.append(':');
            visit(value.getFormat(), p);
        }
        p.append('}');
        return value;
    }

    @Override
    public J visitKeyValue(Py.KeyValue keyValue, PrintOutputCapture<P> p) {
        beforeSyntax(keyValue, p);
        visitRightPadded(keyValue.getPadding().getKey(), p);
        p.append(':');
        visit(keyValue.getValue(), p);
        afterSyntax(keyValue, p);
        return keyValue;
    }

    @Override
    public J visitLiteralType(Py.LiteralType literalType, PrintOutputCapture<P> p) {
        beforeSyntax(literalType, p);
        visit(literalType.getLiteral(), p);
        afterSyntax(literalType, p);
        return literalType;
    }

    @Override
    public J visitMatchCase(Py.MatchCase matchCase, PrintOutputCapture<P> p) {
        beforeSyntax(matchCase, p);
        visit(matchCase.getPattern(), p);
        JLeftPadded<Expression> guard = matchCase.getPadding().getGuard();
        if (guard != null) {
            visitSpace(guard.getBefore(), p);
            p.append("if");
            visit(guard.getElement(), p);
        }
        return matchCase;
    }

    @Override
    public J visitMatchCasePattern(Py.MatchCase.Pattern pattern, PrintOutputCapture<P> p) {
        beforeSyntax(pattern, p);
        JContainer<J> children = pattern.getPadding().getChildren();
        switch (pattern.getKind()) {
            case AS:
                visitContainer("", children, "as", "", p);
                break;
            case CAPTURE:
            case LITERAL:
            case VALUE:
                visitContainer("", children, "", "", p);
                break;
            case CLASS:
                visitSpace(children.getBefore(), p);
                List<JRightPadded<J>> elements = children.getPadding().getElements();
                visitRightPadded(elements.get(0), p);
                visitContainer("(", JContainer.build(Space.EMPTY, elements.subList(1, elements.size()), Markers.EMPTY), ",", ")", p);
                break;
            case DOUBLE_STAR:
                visitContainer("**", children, "", "", p);
                break;
            case KEY_VALUE:
                visitContainer("", children, ":", "", p);
                break;
            case KEYWORD:
                visitContainer("", children, "=", "", p);
                break;
            case MAPPING:
                visitContainer("{", children, ",", "}", p);
                break;
            case OR:
                visitContainer("", children, "|", "", p);
                break;
            case SEQUENCE:
                visitContainer("", children, ",", "", p);
                break;
            case SEQUENCE_LIST:
                visitContainer("[", children, ",", "]", p);
                break;
            case GROUP:
            case SEQUENCE_TUPLE:
                visitContainer("(", children, ",", ")", p);
                break;
            case STAR:
                visitContainer("*", children, "", "", p);
                break;
            case WILDCARD:
                visitContainer("_", children, "", "", p);
                break;
        }
        return pattern;
    }

    @Override
    public J visitMultiImport(Py.MultiImport multiImport, PrintOutputCapture<P> p) {
        beforeSyntax(multiImport, p);
        JRightPadded<NameTree> from = multiImport.getPadding().getFrom();
        if (from != null) {
            p.append("from");
            visitRightPadded(from, p);
        }
        p.append("import");
        if (multiImport.isParenthesized()) {
            visitContainer("(", multiImport.getPadding().getNames(), ",", ")", p);
        } else {
            visitContainer("", multiImport.getPadding().getNames(), ",", "", p);
        }
        afterSyntax(multiImport, p);
        return multiImport;
    }

    @Override
    public J visitNamedArgument(Py.NamedArgument namedArgument, PrintOutputCapture<P> p) {
        beforeSyntax(namedArgument, p);
        visit(namedArgument.getName(), p);
        visitLeftPadded("=", namedArgument.getPadding().getValue(), p);
        return namedArgument;
    }

    @Override
    public J visitPass(Py.Pass pass, PrintOutputCapture<P> p) {
        beforeSyntax(pass, p);
        p.append("pass");
        afterSyntax(pass, p);
        return pass;
    }

    @Override
    public J visitSlice(Py.Slice slice, PrintOutputCapture<P> p) {
        beforeSyntax(slice, p);
        JRightPadded<Expression> start = slice.getPadding().getStart();
        if (start != null) {
            visitRightPadded(start, p);
        }
        p.append(':');
        JRightPadded<Expression> stop = slice.getPadding().getStop();
        if (stop != null) {
            visitRightPadded(stop, p);
        }
        JRightPadded<Expression> step = slice.getPadding().getStep();
        if (step != null) {
            p.append(':');
            visitRightPadded(step, p);
        }
        return slice;
    }

    @Override
    public J visitSpecialParameter(Py.SpecialParameter specialParameter, PrintOutputCapture<P> p) {
        beforeSyntax(specialParameter, p);
        switch (specialParameter.getKind()) {
            case ARGS:
                p.append("*");
                break;
            case KWARGS:
                p.append("**");
                break;
        }
        afterSyntax(specialParameter, p);
        return specialParameter;
    }

    @Override
    public J visitStar(Py.Star star, PrintOutputCapture<P> p) {
        beforeSyntax(star, p);
        switch (star.getKind()) {
            case LIST:
                p.append("*");
                break;
            case DICT:
                p.append("**");
                break;
        }
        visit(star.getExpression(), p);
        afterSyntax(star, p);
        return star;
    }

    @Override
    public J visitStatementExpression(Py.StatementExpression statementExpression, PrintOutputCapture<P> p) {
        visit(statementExpression.getStatement(), p);
        return statementExpression;
    }

    @Override
    public J visitTrailingElseWrapper(Py.TrailingElseWrapper wrapper, PrintOutputCapture<P> p) {
        beforeSyntax(wrapper, p);
        visit(wrapper.getStatement(), p);
        // a try statement prints the else block itself, ahead of its finally block
        if (!(wrapper.getStatement() instanceof J.Try)) {
            visitSpace(wrapper.getPadding().getElseBlock().getBefore(), p);
            p.append("else");
            visit(wrapper.getElseBlock(), p);
        }
        afterSyntax(wrapper, p);
        return wrapper;
    }

    @Override
    public J visitTypeAlias(Py.TypeAlias typeAlias, PrintOutputCapture<P> p) {
        beforeSyntax(typeAlias, p);
        p.append("type");
        visit(typeAlias.getName(), p);
        visitContainer("[", typeAlias.getPadding().getTypeParameters(), ",", "]", p);
        visitLeftPadded("=", typeAlias.getPadding().getValue(), p);
        afterSyntax(typeAlias, p);
        return typeAlias;
    }

    @Override
    public J visitTypeHint(Py.TypeHint typeHint, PrintOutputCapture<P> p) {
        beforeSyntax(typeHint, p);
        if (enclosingTree() instanceof J.MethodDeclaration) {
            p.append("->");
        } else {
            p.append(':');
        }
        visit(typeHint.getTypeTree(), p);
        afterSyntax(typeHint, p);
        return typeHint;
    }

    @Override
    public J visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, PrintOutputCapture<P> p) {
        beforeSyntax(typeHintedExpression, p);
        visit(typeHintedExpression.getExpression(), p);
        visit(typeHintedExpression.getTypeHint(), p);
        afterSyntax(typeHintedExpression, p);
        return typeHintedExpression;
    }

    @Override
    public J visitUnionType(Py.UnionType unionType, PrintOutputCapture<P> p) {
        beforeSyntax(unionType, p);
        visitRightPadded(unionType.getPadding().getTypes(), "|", p);
        afterSyntax(unionType, p);
        return unionType;
    }

    @Override
    public J visitVariableScope(Py.VariableScope variableScope, PrintOutputCapture<P> p) {
        beforeSyntax(variableScope, p);
        switch (variableScope.getKind()) {
            case GLOBAL:
                p.append("global");
                break;
            case NONLOCAL:
                p.append("nonlocal");
                break;
        }
        visitRightPadded(variableScope.getPadding().getNames(), ",", p);
        return variableScope;
    }

    @Override
    public J visitYieldFrom(Py.YieldFrom yieldFrom, PrintOutputCapture<P> p) {
        beforeSyntax(yieldFrom, p);
        p.append("from");
        visit(yieldFrom.getExpression(), p);
        return yieldFrom;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        return delegate.visitMarker(marker, p);
    }

    private void beforeSyntax(J tree, PrintOutputCapture<P> p) {
        delegate.beforeSyntax(tree, p);
    }

    private void afterSyntax(J tree, PrintOutputCapture<P> p) {
        delegate.afterSyntax(tree, p);
    }

    private void visitSpace(@Nullable Space space, PrintOutputCapture<P> p) {
        delegate.visitSpace(space, p);
    }

    private void visitRightPadded(JRightPadded<? extends J> padded, PrintOutputCapture<P> p) {
        delegate.visitRightPadded(padded, p);
    }

    private void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        delegate.visitRightPadded(nodes, suffixBetween, p);
    }

    private void visitLeftPadded(String prefix, JLeftPadded<? extends J> padded, PrintOutputCapture<P> p) {
        delegate.visitLeftPadded(prefix, padded, p);
    }

    private void visitContainer(String before, @Nullable JContainer<? extends J> container, String suffixBetween,
                                String after, PrintOutputCapture<P> p) {
        delegate.visitContainer(before, container, suffixBetween, after, p);
    }

    private @Nullable Tree enclosingTree() {
        // a caller's cursor may hold padding between a tree and its parent
        for (Cursor c = getCursor().getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof Tree) {
                return c.getValue();
            }
        }
        return null;
    }

    private static boolean hasLegacySpelling(J tree, Class<? extends Marker> spelling) {
        for (Marker marker : tree.getMarkers().getMarkers()) {
            // an LST stored while the Python 2 spellings had no codec holds null where one was attached
            if (marker == null || spelling.isInstance(marker)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String quotesAround(J tree) {
        // a literal's value source already holds its delimiters
        if (tree instanceof J.Literal) {
            return null;
        }
        return tree.getMarkers().findFirst(Quoted.class).map(quoted -> quoted.getStyle().getQuote()).orElse(null);
    }

    private class PythonJavaPrinter extends JavaPrinter<P> {
        @Override
        public @Nullable J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof Py) {
                // re-route printing back up to python
                return PythonPrinter.this.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public void setCursor(@Nullable Cursor cursor) {
            // both printers consult the cursor, so they share one
            super.setCursor(cursor);
            PythonPrinter.super.setCursor(cursor);
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
            beforeSyntax(annotation, p);
            p.append("@");
            visit(annotation.getAnnotationType(), p);
            visitContainer("(", annotation.getPadding().getArguments(), ",", ")", p);
            afterSyntax(annotation, p);
            return annotation;
        }

        @Override
        public J visitArrayAccess(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
            beforeSyntax(arrayAccess, p);
            visit(arrayAccess.getIndexed(), p);
            visit(arrayAccess.getDimension(), p);
            afterSyntax(arrayAccess, p);
            return arrayAccess;
        }

        @Override
        public J visitArrayDimension(J.ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
            beforeSyntax(arrayDimension, p);
            p.append("[");
            visitRightPadded(arrayDimension.getPadding().getIndex(), "]", p);
            afterSyntax(arrayDimension, p);
            return arrayDimension;
        }

        @Override
        public J visitAssert(J.Assert assert_, PrintOutputCapture<P> p) {
            beforeSyntax(assert_, p);
            p.append("assert");
            visit(assert_.getCondition(), p);
            if (assert_.getDetail() != null) {
                visitLeftPadded(",", assert_.getDetail(), p);
            }
            afterSyntax(assert_, p);
            return assert_;
        }

        @Override
        public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
            Tree parent = enclosingTree();
            // anywhere other than in statement position, an assignment is a walrus
            boolean regularAssignment = parent instanceof J.Block ||
                    parent instanceof Py.CompilationUnit ||
                    parent instanceof Py.ExpressionStatement ||
                    (parent instanceof J.If && ((J.If) parent).getThenPart().isScope(assignment)) ||
                    (parent instanceof J.If.Else && ((J.If.Else) parent).getBody().isScope(assignment)) ||
                    (parent instanceof Loop && ((Loop) parent).getBody().isScope(assignment));

            beforeSyntax(assignment, p);
            visit(assignment.getVariable(), p);
            visitLeftPadded(regularAssignment ? "=" : ":=", assignment.getPadding().getAssignment(), p);
            afterSyntax(assignment, p);
            return assignment;
        }

        @Override
        public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (assignOp.getOperator()) {
                case Addition:
                    keyword = "+=";
                    break;
                case Subtraction:
                    keyword = "-=";
                    break;
                case Multiplication:
                    keyword = "*=";
                    break;
                case Division:
                    keyword = "/=";
                    break;
                case Modulo:
                    keyword = "%=";
                    break;
                case BitAnd:
                    keyword = "&=";
                    break;
                case BitOr:
                    keyword = "|=";
                    break;
                case BitXor:
                    keyword = "^=";
                    break;
                case LeftShift:
                    keyword = "<<=";
                    break;
                case RightShift:
                    keyword = ">>=";
                    break;
                case UnsignedRightShift:
                    keyword = ">>>=";
                    break;
                case Exponentiation:
                    keyword = "**=";
                    break;
                case FloorDivision:
                    keyword = "//=";
                    break;
                case MatrixMultiplication:
                    keyword = "@=";
                    break;
            }
            beforeSyntax(assignOp, p);
            visit(assignOp.getVariable(), p);
            visitSpace(assignOp.getPadding().getOperator().getBefore(), p);
            p.append(keyword);
            visit(assignOp.getAssignment(), p);
            afterSyntax(assignOp, p);
            return assignOp;
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (binary.getOperator()) {
                case Addition:
                    keyword = "+";
                    break;
                case Subtraction:
                    keyword = "-";
                    break;
                case Multiplication:
                    keyword = "*";
                    break;
                case Division:
                    keyword = "/";
                    break;
                case Modulo:
                    keyword = "%";
                    break;
                case LessThan:
                    keyword = "<";
                    break;
                case GreaterThan:
                    keyword = ">";
                    break;
                case LessThanOrEqual:
                    keyword = "<=";
                    break;
                case GreaterThanOrEqual:
                    keyword = ">=";
                    break;
                case Equal:
                    keyword = "==";
                    break;
                case NotEqual:
                    keyword = hasLegacySpelling(binary, LegacyNotEqual.class) ? "<>" : "!=";
                    break;
                case BitAnd:
                    keyword = "&";
                    break;
                case BitOr:
                    keyword = "|";
                    break;
                case BitXor:
                    keyword = "^";
                    break;
                case LeftShift:
                    keyword = "<<";
                    break;
                case RightShift:
                    keyword = ">>";
                    break;
                case UnsignedRightShift:
                    keyword = ">>>";
                    break;
                case Or:
                    keyword = "or";
                    break;
                case And:
                    keyword = "and";
                    break;
            }
            beforeSyntax(binary, p);
            visit(binary.getLeft(), p);
            visitSpace(binary.getPadding().getOperator().getBefore(), p);
            p.append(keyword);
            visit(binary.getRight(), p);
            afterSyntax(binary, p);
            return binary;
        }

        @Override
        public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
            beforeSyntax(block, p);
            p.append(':');
            visitRightPadded(block.getPadding().getStatements(), "", p);
            visitSpace(block.getEnd(), p);
            afterSyntax(block, p);
            return block;
        }

        @Override
        public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
            beforeSyntax(breakStatement, p);
            p.append("break");
            afterSyntax(breakStatement, p);
            return breakStatement;
        }

        @Override
        public J visitCase(J.Case case_, PrintOutputCapture<P> p) {
            beforeSyntax(case_, p);
            List<J> labels = case_.getCaseLabels();
            J label = labels.isEmpty() ? null : labels.get(0);
            if (!(label instanceof J.Identifier) || !"default".equals(((J.Identifier) label).getSimpleName())) {
                p.append("case");
            }
            visitContainer("", case_.getPadding().getCaseLabels(), ",", "", p);
            visitSpace(case_.getPadding().getStatements().getBefore(), p);
            visitRightPadded(case_.getPadding().getStatements().getPadding().getElements(), "", p);
            JRightPadded<J> body = case_.getPadding().getBody();
            if (body != null) {
                visitRightPadded(body, body.getElement() instanceof Statement ? "" : ";", p);
            }
            afterSyntax(case_, p);
            return case_;
        }

        @Override
        public J visitCatch(J.Try.Catch catch_, PrintOutputCapture<P> p) {
            beforeSyntax(catch_, p);
            p.append("except");

            boolean legacyComma = hasLegacySpelling(catch_, TupleExceptClause.class);

            J.VariableDeclarations multiVariable = catch_.getParameter().getTree();
            beforeSyntax(multiVariable, p);
            visit(multiVariable.getTypeExpression(), p);
            for (JRightPadded<J.VariableDeclarations.NamedVariable> paddedVariable : multiVariable.getPadding().getVariables()) {
                J.VariableDeclarations.NamedVariable variable = paddedVariable.getElement();
                if (!variable.getSimpleName().isEmpty()) {
                    visitSpace(paddedVariable.getAfter(), p);
                    beforeSyntax(variable, p);
                    p.append(legacyComma ? "," : "as");
                    visit(variable.getName(), p);
                    afterSyntax(variable, p);
                }
            }
            afterSyntax(multiVariable, p);

            visit(catch_.getBody(), p);
            afterSyntax(catch_, p);
            return catch_;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
            beforeSyntax(classDecl, p);
            for (J.Annotation annotation : classDecl.getLeadingAnnotations()) {
                visit(annotation, p);
            }
            J.ClassDeclaration.Kind kind = classDecl.getPadding().getKind();
            for (J.Annotation annotation : kind.getAnnotations()) {
                visit(annotation, p);
            }
            visitSpace(kind.getPrefix(), p);
            p.append("class");
            visit(classDecl.getName(), p);
            visitContainer("[", classDecl.getPadding().getTypeParameters(), ",", "]", p);
            JContainer<TypeTree> bases = classDecl.getPadding().getImplements();
            if (bases != null) {
                boolean omitParens = bases.getMarkers().findFirst(OmitParentheses.class).isPresent();
                visitContainer(omitParens ? "" : "(", bases, ",", omitParens ? "" : ")", p);
            }
            visit(classDecl.getBody(), p);
            afterSyntax(classDecl, p);
            return classDecl;
        }

        @Override
        public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
            beforeSyntax(continueStatement, p);
            p.append("continue");
            afterSyntax(continueStatement, p);
            return continueStatement;
        }

        @Override
        public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
            beforeSyntax(controlParens, p);
            visitRightPadded(controlParens.getPadding().getTree(), p);
            afterSyntax(controlParens, p);
            return controlParens;
        }

        @Override
        public J visitElse(J.If.Else else_, PrintOutputCapture<P> p) {
            beforeSyntax(else_, p);
            if (enclosingTree() instanceof J.If && else_.getBody() instanceof J.If) {
                // the nested if completes the elif
                p.append("el");
            } else if (else_.getBody() instanceof J.Block) {
                p.append("else");
            } else {
                p.append("else");
                p.append(':');
            }
            visit(else_.getBody(), p);
            afterSyntax(else_, p);
            return else_;
        }

        @Override
        public J visitEmpty(J.Empty empty, PrintOutputCapture<P> p) {
            beforeSyntax(empty, p);
            afterSyntax(empty, p);
            return empty;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(".", fieldAccess.getPadding().getName(), p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitForEachControl(J.ForEachLoop.Control control, PrintOutputCapture<P> p) {
            beforeSyntax(control, p);
            visitRightPadded(control.getPadding().getVariable(), p);
            p.append("in");
            visitRightPadded(control.getPadding().getIterable(), p);
            afterSyntax(control, p);
            return control;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forEachLoop, p);
            p.append("for");
            visit(forEachLoop.getControl(), p);
            printLoopBody(forEachLoop.getBody(), p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            beforeSyntax(ident, p);
            p.append(ident.getSimpleName());
            afterSyntax(ident, p);
            return ident;
        }

        @Override
        public J visitIf(J.If iff, PrintOutputCapture<P> p) {
            beforeSyntax(iff, p);
            p.append("if");
            visit(iff.getIfCondition(), p);
            JRightPadded<Statement> thenPart = iff.getPadding().getThenPart();
            if (!(thenPart.getElement() instanceof J.Block)) {
                p.append(":");
            }
            visitRightPadded(thenPart, p);
            visit(iff.getElsePart(), p);
            afterSyntax(iff, p);
            return iff;
        }

        @Override
        public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
            beforeSyntax(import_, p);
            boolean standalone = getCursor().firstEnclosing(Py.MultiImport.class) == null;
            if (standalone) {
                p.append("import");
            }
            J.FieldAccess qualid = import_.getQualid();
            if (qualid.getTarget() instanceof J.Empty) {
                if (standalone) {
                    visitSpace(qualid.getPrefix(), p);
                }
                visit(qualid.getName(), p);
            } else {
                visit(qualid, p);
            }
            if (import_.getPadding().getAlias() != null) {
                visitLeftPadded("as", import_.getPadding().getAlias(), p);
            }
            afterSyntax(import_, p);
            return import_;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, p);
            p.append("lambda");
            visit(lambda.getParameters(), p);
            visitSpace(lambda.getArrow(), p);
            p.append(":");
            visit(lambda.getBody(), p);
            afterSyntax(lambda, p);
            return lambda;
        }

        @Override
        public J visitLambdaParameters(J.Lambda.Parameters parameters, PrintOutputCapture<P> p) {
            visitSpace(parameters.getPrefix(), p);
            visitMarkers(parameters.getMarkers(), p);
            visitRightPadded(parameters.getPadding().getParameters(), ",", p);
            return parameters;
        }

        @Override
        public J visitLiteral(J.Literal literal, PrintOutputCapture<P> p) {
            String valueSource = literal.getValueSource();
            if (literal.getValue() == null && valueSource == null) {
                valueSource = "None";
            }
            beforeSyntax(literal, p);
            List<J.Literal.UnicodeEscape> unicodeEscapes = literal.getUnicodeEscapes();
            if (unicodeEscapes == null) {
                p.append(valueSource);
            } else if (valueSource != null) {
                Iterator<J.Literal.UnicodeEscape> escapes = unicodeEscapes.iterator();
                J.Literal.UnicodeEscape escape = escapes.hasNext() ? escapes.next() : null;
                // the parser counted the escape positions in code points
                for (int i = 0, offset = 0; offset < valueSource.length(); i++) {
                    while (escape != null && escape.getValueSourceIndex() == i) {
                        p.append("\\u").append(escape.getCodePoint());
                        escape = escapes.hasNext() ? escapes.next() : null;
                    }
                    int codePoint = valueSource.codePointAt(offset);
                    p.out.appendCodePoint(codePoint);
                    offset += Character.charCount(codePoint);
                }
                while (escape != null) {
                    p.append("\\u").append(escape.getCodePoint());
                    escape = escapes.hasNext() ? escapes.next() : null;
                }
            }
            afterSyntax(literal, p);
            return literal;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            beforeSyntax(method, p);
            for (J.Annotation annotation : method.getLeadingAnnotations()) {
                visit(annotation, p);
            }
            for (J.Modifier modifier : method.getModifiers()) {
                visitModifier(modifier, p);
            }
            visit(method.getName(), p);
            visit(method.getPadding().getTypeParameters(), p);
            visitContainer("(", method.getPadding().getParameters(), ",", ")", p);
            visit(method.getReturnTypeExpression(), p);
            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, p);
            PrintSyntax printSyntax = method.getMarkers().findFirst(PrintSyntax.class).orElse(null);
            JContainer<Expression> arguments = method.getPadding().getArguments();
            if (printSyntax != null) {
                p.append("print");
                List<JRightPadded<Expression>> elements = arguments.getPadding().getElements();
                if (printSyntax.isHasDestination() && !elements.isEmpty()) {
                    p.append(" >>");
                }
                for (int i = 0; i < elements.size(); i++) {
                    visitRightPadded(elements.get(i), i < elements.size() - 1 || printSyntax.isTrailingComma() ? "," : "", p);
                }
            } else if (method.getMarkers().findFirst(ExecSyntax.class).isPresent()) {
                p.append("exec");
                List<JRightPadded<Expression>> elements = arguments.getPadding().getElements();
                for (int i = 0; i < elements.size(); i++) {
                    String suffix = "";
                    if (i == 0 && elements.size() > 1) {
                        suffix = "in";
                    } else if (i > 0 && i < elements.size() - 1) {
                        suffix = ",";
                    }
                    visitRightPadded(elements.get(i), suffix, p);
                }
            } else {
                JRightPadded<Expression> select = method.getPadding().getSelect();
                if (select != null) {
                    visitRightPadded(select, method.getSimpleName().isEmpty() ? "" : ".", p);
                }
                visitContainer("<", method.getPadding().getTypeParameters(), ",", ">", p);
                visit(method.getName(), p);
                if (method.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    visitContainer("", arguments, ",", "", p);
                } else {
                    visitContainer("(", arguments, ",", ")", p);
                }
            }
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitModifier(J.Modifier mod, PrintOutputCapture<P> p) {
            String keyword = null;
            switch (mod.getType()) {
                case Default:
                    keyword = "def";
                    break;
                case Async:
                    keyword = "async";
                    break;
                case LanguageExtension:
                    keyword = mod.getKeyword();
                    break;
                default:
                    break;
            }
            if (keyword != null && !keyword.isEmpty()) {
                for (J.Annotation annotation : mod.getAnnotations()) {
                    visit(annotation, p);
                }
                beforeSyntax(mod, p);
                p.append(keyword);
                afterSyntax(mod, p);
            }
            return mod;
        }

        @Override
        public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
            beforeSyntax(newArray, p);
            visitContainer("[", newArray.getPadding().getInitializer(), ",", "]", p);
            afterSyntax(newArray, p);
            return newArray;
        }

        @Override
        public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
            beforeSyntax(type, p);
            visit(type.getClazz(), p);
            visitContainer("[", type.getPadding().getTypeParameters(), ",", "]", p);
            afterSyntax(type, p);
            return type;
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, PrintOutputCapture<P> p) {
            beforeSyntax(parens, p);
            p.append("(");
            visitRightPadded(parens.getPadding().getTree(), ")", p);
            afterSyntax(parens, p);
            return parens;
        }

        @Override
        public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
            beforeSyntax(return_, p);
            p.append("return");
            visit(return_.getExpression(), p);
            afterSyntax(return_, p);
            return return_;
        }

        @Override
        public J visitSwitch(J.Switch switch_, PrintOutputCapture<P> p) {
            beforeSyntax(switch_, p);
            p.append("match");
            visit(switch_.getSelector(), p);
            visit(switch_.getCases(), p);
            afterSyntax(switch_, p);
            return switch_;
        }

        @Override
        public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
            beforeSyntax(ternary, p);
            visit(ternary.getTruePart(), p);
            visitSpace(ternary.getPadding().getTruePart().getBefore(), p);
            p.append("if");
            visit(ternary.getCondition(), p);
            visitLeftPadded("else", ternary.getPadding().getFalsePart(), p);
            afterSyntax(ternary, p);
            return ternary;
        }

        @Override
        public J visitThrow(J.Throw thrown, PrintOutputCapture<P> p) {
            beforeSyntax(thrown, p);
            p.append("raise");
            Expression exception = thrown.getException();
            if (hasLegacySpelling(thrown, RaiseTuple.class) &&
                    exception instanceof Py.CollectionLiteral &&
                    ((Py.CollectionLiteral) exception).getKind() == Py.CollectionLiteral.Kind.TUPLE) {
                // Python 2's `raise E, v, tb` holds its operands in a tuple that prints without parentheses
                visitSpace(exception.getPrefix(), p);
                List<JRightPadded<Expression>> elements = ((Py.CollectionLiteral) exception).getPadding().getElements().getPadding().getElements();
                for (int i = 0; i < elements.size(); i++) {
                    visitRightPadded(elements.get(i), i < elements.size() - 1 ? "," : "", p);
                }
            } else {
                visit(exception, p);
            }
            afterSyntax(thrown, p);
            return thrown;
        }

        @Override
        public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
            JContainer<J.Try.Resource> resources = tryable.getPadding().getResources();
            boolean withStatement = resources != null && !resources.getPadding().getElements().isEmpty();

            beforeSyntax(tryable, p);
            p.append(withStatement ? "with" : "try");

            if (withStatement) {
                visitSpace(resources.getBefore(), p);
                boolean omitParens = resources.getMarkers().findFirst(OmitParentheses.class).isPresent();
                if (!omitParens) {
                    p.append("(");
                }
                boolean first = true;
                for (JRightPadded<J.Try.Resource> resource : resources.getPadding().getElements()) {
                    if (first) {
                        first = false;
                    } else {
                        p.append(",");
                    }
                    visitSpace(resource.getElement().getPrefix(), p);
                    visitMarkers(resource.getElement().getMarkers(), p);

                    TypedTree decl = resource.getElement().getVariableDeclarations();
                    if (decl instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) decl;
                        visit(assignment.getAssignment(), p);
                        if (!(assignment.getVariable() instanceof J.Empty)) {
                            visitSpace(assignment.getPadding().getAssignment().getBefore(), p);
                            p.append("as");
                            visit(assignment.getVariable(), p);
                        }
                    } else {
                        visit(decl, p);
                    }

                    visitSpace(resource.getAfter(), p);
                    visitMarkers(resource.getMarkers(), p);
                }
                visitMarkers(resources.getMarkers(), p);
                if (!omitParens) {
                    p.append(")");
                }
            }

            Tree parent = enclosingTree();
            Py.TrailingElseWrapper elseWrapper = parent instanceof Py.TrailingElseWrapper ?
                    (Py.TrailingElseWrapper) parent : null;

            visit(tryable.getBody(), p);
            for (J.Try.Catch catch_ : tryable.getCatches()) {
                visit(catch_, p);
            }
            if (elseWrapper != null) {
                visitSpace(elseWrapper.getPadding().getElseBlock().getBefore(), p);
                p.append("else");
                visit(elseWrapper.getElseBlock(), p);
            }
            if (tryable.getPadding().getFinally() != null) {
                visitLeftPadded("finally", tryable.getPadding().getFinally(), p);
            }
            afterSyntax(tryable, p);
            return tryable;
        }

        @Override
        public J visitTryResource(J.Try.Resource tryResource, PrintOutputCapture<P> p) {
            beforeSyntax(tryResource, p);
            visit(tryResource.getVariableDeclarations(), p);
            afterSyntax(tryResource, p);
            return tryResource;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            beforeSyntax(typeParam, p);
            for (J.Modifier modifier : typeParam.getModifiers()) {
                visit(modifier, p);
            }
            visit(typeParam.getName(), p);
            // one bound is a constraint, two are a constraint and a default, either of which may be empty
            JContainer<TypeTree> bounds = typeParam.getPadding().getBounds();
            if (bounds != null) {
                List<JRightPadded<TypeTree>> elements = bounds.getPadding().getElements();
                if (elements.size() == 1) {
                    visitSpace(bounds.getBefore(), p);
                    p.append(":");
                    visitRightPadded(elements.get(0), p);
                } else if (elements.size() == 2) {
                    JRightPadded<TypeTree> constraint = elements.get(0);
                    JRightPadded<TypeTree> defaultType = elements.get(1);
                    if (!(constraint.getElement() instanceof J.Empty)) {
                        visitSpace(bounds.getBefore(), p);
                        p.append(":");
                        visitRightPadded(constraint, p);
                    }
                    if (!(defaultType.getElement() instanceof J.Empty)) {
                        if (constraint.getElement() instanceof J.Empty) {
                            visitSpace(bounds.getBefore(), p);
                        }
                        p.append("=");
                        visitRightPadded(defaultType, p);
                    }
                }
            }
            afterSyntax(typeParam, p);
            return typeParam;
        }

        @Override
        public J visitTypeParameters(J.TypeParameters typeParameters, PrintOutputCapture<P> p) {
            for (J.Annotation annotation : typeParameters.getAnnotations()) {
                visit(annotation, p);
            }
            visitSpace(typeParameters.getPrefix(), p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("[");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), ",", p);
            p.append("]");
            return typeParameters;
        }

        @Override
        public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
            beforeSyntax(unary, p);
            switch (unary.getOperator()) {
                case Not:
                    p.append("not");
                    break;
                case Positive:
                    p.append("+");
                    break;
                case Negative:
                    p.append("-");
                    break;
                case Complement:
                    p.append("~");
                    break;
                default:
                    break;
            }
            visit(unary.getExpression(), p);
            afterSyntax(unary, p);
            return unary;
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
            beforeSyntax(variable, p);

            Tree parent = enclosingTree();
            J.VariableDeclarations vd = parent instanceof J.VariableDeclarations ? (J.VariableDeclarations) parent : null;

            TypeTree typeExpr = vd == null ? null : vd.getTypeExpression();
            if (typeExpr instanceof Py.SpecialParameter) {
                Py.SpecialParameter special = (Py.SpecialParameter) typeExpr;
                visit(special, p);
                typeExpr = special.getTypeHint();
            }

            VariableDeclarator name = variable.getDeclarator();
            if (name instanceof J.Identifier && ((J.Identifier) name).getSimpleName().isEmpty()) {
                visit(variable.getInitializer(), p);
            } else {
                if (vd != null && vd.getVarargs() != null) {
                    visitSpace(vd.getVarargs(), p);
                    p.append('*');
                }
                if (vd != null && vd.getMarkers().findFirst(KeywordArguments.class).isPresent()) {
                    p.append("**");
                }
                visit(name, p);
                if (typeExpr instanceof Py.TypeHint) {
                    // a special parameter's hint prints its own colon
                    visit(typeExpr, p);
                } else if (vd != null && typeExpr != null) {
                    // the space ahead of the type hint is the padding after this variable
                    for (JRightPadded<J.VariableDeclarations.NamedVariable> padded : vd.getPadding().getVariables()) {
                        if (padded.getElement().isScope(variable)) {
                            visitSpace(padded.getAfter(), p);
                        }
                    }
                    p.append(':');
                    visit(typeExpr, p);
                }
                if (variable.getPadding().getInitializer() != null) {
                    visitLeftPadded("=", variable.getPadding().getInitializer(), p);
                }
            }

            afterSyntax(variable, p);
            return variable;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            beforeSyntax(multiVariable, p);
            for (J.Annotation annotation : multiVariable.getLeadingAnnotations()) {
                visit(annotation, p);
            }
            for (J.Modifier modifier : multiVariable.getModifiers()) {
                visitModifier(modifier, p);
            }

            boolean keywordOnly = multiVariable.getMarkers().findFirst(KeywordOnlyArguments.class).isPresent();
            if (keywordOnly) {
                p.append("*");
            }

            List<JRightPadded<J.VariableDeclarations.NamedVariable>> nodes = multiVariable.getPadding().getVariables();
            for (int i = 0; i < nodes.size(); i++) {
                JRightPadded<J.VariableDeclarations.NamedVariable> node = nodes.get(i);
                visit(node.getElement(), p);
                visitMarkers(node.getMarkers(), p);
                // otherwise the space after the name precedes the type hint, which visitVariable prints
                if (keywordOnly) {
                    visitSpace(node.getAfter(), p);
                }
                if (i < nodes.size() - 1) {
                    p.append(",");
                }
            }

            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        @Override
        public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
            beforeSyntax(whileLoop, p);
            p.append("while");
            visit(whileLoop.getCondition(), p);
            printLoopBody(whileLoop.getBody(), p);
            afterSyntax(whileLoop, p);
            return whileLoop;
        }

        private void printLoopBody(Statement body, PrintOutputCapture<P> p) {
            // a block prints its own colon
            if (!(body instanceof J.Block)) {
                p.append(':');
            }
            visit(body, p);
        }

        @Override
        public J visitYield(J.Yield yield, PrintOutputCapture<P> p) {
            beforeSyntax(yield, p);
            p.append("yield");
            visit(yield.getValue(), p);
            afterSyntax(yield, p);
            return yield;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
            visitSpace(space, p);
            return space;
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.append(';');
            } else if (marker instanceof TrailingComma) {
                p.append(',');
                visitSpace(((TrailingComma) marker).getSuffix(), p);
            }
            //noinspection unchecked
            return (M) marker;
        }

        private void beforeSyntax(J tree, PrintOutputCapture<P> p) {
            List<Marker> markers = tree.getMarkers().getMarkers();
            for (Marker marker : markers) {
                // LSTs stored while a marker had no codec hold null in its place
                if (marker != null) {
                    p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
                }
            }
            visitSpace(tree.getPrefix(), p);
            visitMarkers(tree.getMarkers(), p);
            for (Marker marker : markers) {
                if (marker != null) {
                    p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
                }
            }
            p.append(quotesAround(tree));
        }

        @Override
        protected void afterSyntax(J tree, PrintOutputCapture<P> p) {
            p.append(quotesAround(tree));
            for (Marker marker : tree.getMarkers().getMarkers()) {
                if (marker != null) {
                    p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
                }
            }
        }

        private void visitSpace(@Nullable Space space, PrintOutputCapture<P> p) {
            if (space == null) {
                return;
            }
            p.append(space.getWhitespace());
            for (Comment comment : space.getComments()) {
                if (comment instanceof TextComment) {
                    // what arrives from the Python parser, where a multiline comment is a docstring
                    TextComment text = (TextComment) comment;
                    if (text.isMultiline()) {
                        p.append("\"\"\"").append(text.getText()).append("\"\"\"");
                    } else {
                        p.append('#').append(text.getText());
                    }
                } else {
                    comment.printComment(getCursor(), p);
                }
                p.append(comment.getSuffix());
            }
        }

        private void visitRightPadded(JRightPadded<? extends J> padded, PrintOutputCapture<P> p) {
            visitRightPadded(padded, "", p);
        }

        private void visitRightPadded(JRightPadded<? extends J> padded, String suffix, PrintOutputCapture<P> p) {
            visit(padded.getElement(), p);
            visitSpace(padded.getAfter(), p);
            visitMarkers(padded.getMarkers(), p);
            p.append(suffix);
        }

        private void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
            for (int i = 0; i < nodes.size(); i++) {
                JRightPadded<? extends J> node = nodes.get(i);
                visit(node.getElement(), p);
                visitSpace(node.getAfter(), p);
                visitMarkers(node.getMarkers(), p);
                if (i < nodes.size() - 1) {
                    p.append(suffixBetween);
                }
            }
        }

        private void visitLeftPadded(String prefix, JLeftPadded<? extends J> padded, PrintOutputCapture<P> p) {
            visitSpace(padded.getBefore(), p);
            p.append(prefix);
            visit(padded.getElement(), p);
            visitMarkers(padded.getMarkers(), p);
        }

        private void visitContainer(String before, @Nullable JContainer<? extends J> container, String suffixBetween,
                                    String after, PrintOutputCapture<P> p) {
            if (container == null) {
                return;
            }
            visitSpace(container.getBefore(), p);
            p.append(before);
            visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
            p.append(after);
        }
    }
}
