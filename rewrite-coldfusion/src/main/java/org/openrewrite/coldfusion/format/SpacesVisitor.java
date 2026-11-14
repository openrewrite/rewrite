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
package org.openrewrite.coldfusion.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.marker.AttributeClause;
import org.openrewrite.coldfusion.marker.FunctionKeyword;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.OmitComma;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.marker.VarKeyword;
import org.openrewrite.coldfusion.style.SpacesStyle;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

/**
 * Puts one space or none between the tokens of a line, as the {@link SpacesStyle} says. A space that holds a line
 * break or a comment stays as it is, and so does the spacing an author wrote inside a string or a
 * {@code #interpolation#}. A space is never removed where the tokens on each side would run together, as in
 * {@code a - -1}.
 */
public class SpacesVisitor<P> extends FormattingVisitor<P> {
    private final SpacesStyle style;

    public SpacesVisitor(SpacesStyle style) {
        this(style, null);
    }

    public SpacesVisitor(SpacesStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    private boolean keepsSpacing() {
        return getCursor().getNearestMessage("stop") != null || isInInterpolation();
    }

    // ------------------------------------------------------------------------------------------
    // Operators
    // ------------------------------------------------------------------------------------------

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary b = super.visitBinary(binary, p);
        if (keepsSpacing()) {
            return b;
        }
        String operator = Operators.binary(b);
        boolean padded = Operators.isWord(operator) || style.getAroundOperators().getBinary();
        b = b.getPadding().withOperator(b.getPadding().getOperator().withBefore(
                spaced(b.getPadding().getOperator().getBefore(), padded, Tokens.last(b.getLeft(), getCursor()), operator.charAt(0))));
        return b.withRight(b.getRight().withPrefix(
                spaced(b.getRight().getPrefix(), padded, operator.charAt(operator.length() - 1), Tokens.first(b.getRight(), getCursor()))));
    }

    @Override
    public Cf.Binary visitBinary(Cf.Binary binary, P p) {
        Cf.Binary b = super.visitBinary(binary, p);
        if (keepsSpacing()) {
            return b;
        }
        String operator = Operators.binary(b);
        boolean padded = Operators.isWord(operator) || style.getAroundOperators().getBinary();
        b = b.getPadding().withOperator(b.getPadding().getOperator().withBefore(
                spaced(b.getPadding().getOperator().getBefore(), padded, Tokens.last(b.getLeft(), getCursor()), operator.charAt(0))));
        return b.withRight(b.getRight().withPrefix(
                spaced(b.getRight().getPrefix(), padded, operator.charAt(operator.length() - 1), Tokens.first(b.getRight(), getCursor()))));
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        if (keepsSpacing()) {
            return u;
        }
        String operator = Operators.unary(u);
        if (u.getOperator() == J.Unary.Type.PostIncrement || u.getOperator() == J.Unary.Type.PostDecrement) {
            return u.getPadding().withOperator(u.getPadding().getOperator().withBefore(
                    spaced(u.getPadding().getOperator().getBefore(), false, Tokens.last(u.getExpression(), getCursor()), operator.charAt(0))));
        }
        return u.withExpression(u.getExpression().withPrefix(spaced(u.getExpression().getPrefix(), Operators.isWord(operator),
                operator.charAt(operator.length() - 1), Tokens.first(u.getExpression(), getCursor()))));
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        if (keepsSpacing()) {
            return a;
        }
        if (a.getMarkers().findFirst(VarKeyword.class).isPresent()) {
            a = a.withVariable(a.getVariable().withPrefix(single(a.getVariable().getPrefix())));
        }
        boolean padded = style.getAroundOperators().getAssignment();
        JLeftPadded<Expression> value = a.getPadding().getAssignment();
        if (isAligned(value.getBefore())) {
            return a;
        }
        value = value.withBefore(spaced(value.getBefore(), padded, Tokens.last(a.getVariable(), getCursor()), '='));
        value = value.withElement(value.getElement().withPrefix(
                spaced(value.getElement().getPrefix(), padded, '=', Tokens.first(value.getElement(), getCursor()))));
        return a.getPadding().withAssignment(value);
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        if (keepsSpacing()) {
            return a;
        }
        if (a.getMarkers().findFirst(VarKeyword.class).isPresent()) {
            a = a.withVariable(a.getVariable().withPrefix(single(a.getVariable().getPrefix())));
        }
        boolean padded = style.getAroundOperators().getAssignment();
        String operator = Operators.assignment(a.getOperator());
        if (isAligned(a.getPadding().getOperator().getBefore())) {
            return a;
        }
        a = a.getPadding().withOperator(a.getPadding().getOperator().withBefore(
                spaced(a.getPadding().getOperator().getBefore(), padded, Tokens.last(a.getVariable(), getCursor()), operator.charAt(0))));
        return a.withAssignment(a.getAssignment().withPrefix(
                spaced(a.getAssignment().getPrefix(), padded, '=', Tokens.first(a.getAssignment(), getCursor()))));
    }

    @Override
    public Cf.AssignmentOperation visitAssignmentOperation(Cf.AssignmentOperation assignOp, P p) {
        Cf.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        if (keepsSpacing()) {
            return a;
        }
        if (a.getMarkers().findFirst(VarKeyword.class).isPresent()) {
            a = a.withVariable(a.getVariable().withPrefix(single(a.getVariable().getPrefix())));
        }
        boolean padded = style.getAroundOperators().getAssignment();
        if (isAligned(a.getPadding().getOperator().getBefore())) {
            return a;
        }
        a = a.getPadding().withOperator(a.getPadding().getOperator().withBefore(
                spaced(a.getPadding().getOperator().getBefore(), padded, Tokens.last(a.getVariable(), getCursor()), '&')));
        return a.withAssignment(a.getAssignment().withPrefix(
                spaced(a.getAssignment().getPrefix(), padded, '=', Tokens.first(a.getAssignment(), getCursor()))));
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        if (keepsSpacing()) {
            return v;
        }
        if (v.getMarkers().findFirst(VarKeyword.class).isPresent()) {
            v = v.withName(v.getName().withPrefix(single(v.getName().getPrefix())));
        }
        JLeftPadded<Expression> initializer = v.getPadding().getInitializer();
        if (initializer != null && !isAligned(initializer.getBefore())) {
            boolean padded = style.getAroundOperators().getAssignment();
            initializer = initializer.withBefore(spaced(initializer.getBefore(), padded, Tokens.last(v.getName(), getCursor()), '='));
            initializer = initializer.withElement(initializer.getElement().withPrefix(
                    spaced(initializer.getElement().getPrefix(), padded, '=', Tokens.first(initializer.getElement(), getCursor()))));
            v = v.getPadding().withInitializer(initializer);
        }
        return v;
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = super.visitTernary(ternary, p);
        if (keepsSpacing()) {
            return t;
        }
        boolean padded = style.getAroundOperators().getTernary();
        JLeftPadded<Expression> truePart = t.getPadding().getTruePart();
        truePart = truePart.withBefore(spaced(truePart.getBefore(), padded, Tokens.last(t.getCondition(), getCursor()), '?'));
        truePart = truePart.withElement(truePart.getElement().withPrefix(
                spaced(truePart.getElement().getPrefix(), padded, '?', Tokens.first(truePart.getElement(), getCursor()))));
        JLeftPadded<Expression> falsePart = t.getPadding().getFalsePart();
        falsePart = falsePart.withBefore(spaced(falsePart.getBefore(), padded, Tokens.last(truePart.getElement(), getCursor()), ':'));
        falsePart = falsePart.withElement(falsePart.getElement().withPrefix(
                spaced(falsePart.getElement().getPrefix(), padded, ':', Tokens.first(falsePart.getElement(), getCursor()))));
        return t.getPadding().withTruePart(truePart).getPadding().withFalsePart(falsePart);
    }

    // ------------------------------------------------------------------------------------------
    // Functions, calls, members, literals
    // ------------------------------------------------------------------------------------------

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = super.visitLambda(lambda, p);
        if (keepsSpacing()) {
            return l;
        }
        J.Lambda.Parameters parameters = l.getParameters();
        boolean function = l.getMarkers().findFirst(FunctionKeyword.class).isPresent();
        if (function) {
            parameters = parameters.withPrefix(spaced(parameters.getPrefix(), style.getBeforeParentheses().getAnonymousFunction()));
        }
        if (parameters.isParenthesized()) {
            parameters = parameters.getPadding().withParameters(delimited(parameters.getPadding().getParameters(),
                    style.getWithin().getAnonymousFunctionParentheses(), style.getWithin().getEmptyFunctionDeclarationParentheses()));
        }
        l = l.withParameters(parameters);
        if (function) {
            return l.getBody() instanceof J.Block ? l.withBody(brace((J.Block) l.getBody(), style.getOther().getBeforeClosureBrace())) : l;
        }
        boolean padded = style.getAroundOperators().getArrow();
        l = l.withArrow(spaced(l.getArrow(), padded));
        return l.withBody(l.getBody().withPrefix(spaced(l.getBody().getPrefix(), padded, '>', Tokens.first(l.getBody(), getCursor()))));
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        if (keepsSpacing()) {
            return m;
        }
        boolean padded = attributeEquals(m);
        m = m.withLeadingAnnotations(attributes(m.getLeadingAnnotations(), padded));
        m = m.withMarkers(clauses(m.getMarkers(), padded));
        if (TemplateOutput.isTagSyntax(m)) {
            return m;
        }
        m = m.withModifiers(ListUtils.map(m.getModifiers(), (i, modifier) -> i == 0 ? modifier : modifier.withPrefix(single(modifier.getPrefix()))));
        if (m.getReturnTypeExpression() != null && !m.getModifiers().isEmpty()) {
            m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(single(m.getReturnTypeExpression().getPrefix())));
        }
        FunctionKeyword keyword = m.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
        if (keyword != null) {
            m = m.withMarkers(m.getMarkers().setByType(keyword.withPrefix(single(keyword.getPrefix()))));
        }
        m = m.withName(m.getName().withPrefix(single(m.getName().getPrefix())));
        JContainer<Statement> parameters = m.getPadding().getParameters();
        parameters = parameters.getPadding().withElements(delimited(parameters.getPadding().getElements(),
                style.getWithin().getFunctionDeclarationParentheses(), style.getWithin().getEmptyFunctionDeclarationParentheses()));
        m = m.getPadding().withParameters(parameters.withBefore(spaced(parameters.getBefore(), style.getBeforeParentheses().getFunctionDeclaration())));
        return m.getBody() == null ? m : m.withBody(brace(m.getBody(), style.getOther().getBeforeFunctionBrace()));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        if (keepsSpacing()) {
            return m;
        }
        JRightPadded<Expression> select = m.getPadding().getSelect();
        if (select != null) {
            m = m.getPadding().withSelect(select.withAfter(spaced(select.getAfter(), false)));
            m = m.withName(m.getName().withPrefix(spaced(m.getName().getPrefix(), false)));
        }
        JContainer<Expression> arguments = m.getPadding().getArguments();
        if (arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            return m;
        }
        return m.getPadding().withArguments(arguments(arguments));
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = super.visitNewClass(newClass, p);
        if (keepsSpacing()) {
            return n;
        }
        if (n.getClazz() != null) {
            n = n.withClazz(n.getClazz().withPrefix(single(n.getClazz().getPrefix())));
        }
        JContainer<Expression> arguments = n.getPadding().getArguments();
        if (arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            return n;
        }
        return n.getPadding().withArguments(arguments(arguments));
    }

    @Override
    public Cf.Invocation visitInvocation(Cf.Invocation invocation, P p) {
        Cf.Invocation i = super.visitInvocation(invocation, p);
        if (keepsSpacing()) {
            return i;
        }
        return i.getPadding().withArguments(arguments(i.getPadding().getArguments()));
    }

    private <T extends J> JContainer<T> arguments(JContainer<T> arguments) {
        JContainer<T> a = arguments.getPadding().withElements(delimited(arguments.getPadding().getElements(),
                style.getWithin().getFunctionCallParentheses(), style.getWithin().getEmptyFunctionCallParentheses()));
        return a.withBefore(spaced(a.getBefore(), style.getBeforeParentheses().getFunctionCall()));
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (keepsSpacing()) {
            return f;
        }
        JLeftPadded<J.Identifier> name = f.getPadding().getName();
        return f.getPadding().withName(name.withBefore(spaced(name.getBefore(), false))
                .withElement(name.getElement().withPrefix(spaced(name.getElement().getPrefix(), false))));
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = super.visitArrayAccess(arrayAccess, p);
        if (keepsSpacing()) {
            return a;
        }
        J.ArrayDimension dimension = a.getDimension();
        boolean padded = style.getWithin().getBrackets();
        JRightPadded<Expression> index = dimension.getPadding().getIndex();
        index = index.withElement(index.getElement().withPrefix(spaced(index.getElement().getPrefix(), padded))).withAfter(spaced(index.getAfter(), padded));
        return a.withDimension(dimension.withPrefix(spaced(dimension.getPrefix(), false)).getPadding().withIndex(index));
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = super.visitNewArray(newArray, p);
        JContainer<Expression> initializer = n.getPadding().getInitializer();
        if (keepsSpacing() || initializer == null || n.getTypeExpression() != null) {
            return n;
        }
        return n.getPadding().withInitializer(initializer.getPadding().withElements(delimited(initializer.getPadding().getElements(),
                style.getWithin().getArrayLiteral(), style.getWithin().getEmptyArrayLiteral())));
    }

    @Override
    public Cf.StructLiteral visitStructLiteral(Cf.StructLiteral structLiteral, P p) {
        Cf.StructLiteral s = super.visitStructLiteral(structLiteral, p);
        if (keepsSpacing()) {
            return s;
        }
        List<Expression> entries = s.getEntries();
        if (s.isOrdered() && entries.size() == 1 && entries.get(0) instanceof Cf.KeyValue &&
            ((Cf.KeyValue) entries.get(0)).getKey() instanceof J.Empty) {
            // [:], the empty ordered struct
            return s;
        }
        JContainer<Expression> container = s.getPadding().getEntries();
        return s.getPadding().withEntries(container.getPadding().withElements(delimited(container.getPadding().getElements(),
                style.getWithin().getStructLiteral(), style.getWithin().getEmptyStructLiteral())));
    }

    @Override
    public Cf.KeyValue visitKeyValue(Cf.KeyValue keyValue, P p) {
        Cf.KeyValue k = super.visitKeyValue(keyValue, p);
        if (keepsSpacing() || k.getKey() instanceof J.Empty) {
            return k;
        }
        boolean colon = k.getSeparator() == Cf.KeyValue.Separator.Colon;
        boolean before = colon ? style.getOther().getBeforeColon() : style.getOther().getAroundKeyValueEquals();
        boolean after = colon ? style.getOther().getAfterColon() : style.getOther().getAroundKeyValueEquals();
        char separator = colon ? ':' : '=';
        JLeftPadded<Cf.KeyValue.Separator> s = k.getPadding().getSeparator();
        if (isAligned(s.getBefore())) {
            return k;
        }
        k = k.getPadding().withSeparator(s.withBefore(spaced(s.getBefore(), before, Tokens.last(k.getKey(), getCursor()), separator)));
        return k.withValue(k.getValue().withPrefix(spaced(k.getValue().getPrefix(), after, separator, Tokens.first(k.getValue(), getCursor()))));
    }

    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> parentheses = super.visitParentheses(parens, p);
        if (keepsSpacing()) {
            return parentheses;
        }
        boolean padded = style.getWithin().getGroupingParentheses();
        JRightPadded<T> tree = parentheses.getPadding().getTree();
        return parentheses.getPadding().withTree(tree.withElement(tree.getElement().withPrefix(spaced(tree.getElement().getPrefix(), padded)))
                .withAfter(spaced(tree.getAfter(), padded)));
    }

    // ------------------------------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------------------------------

    @Override
    public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        J.ControlParentheses<T> c = super.visitControlParentheses(controlParens, p);
        if (keepsSpacing()) {
            return c;
        }
        boolean padded = style.getWithin().getKeywordParentheses();
        JRightPadded<T> tree = c.getPadding().getTree();
        c = c.getPadding().withTree(tree.withElement(tree.getElement().withPrefix(spaced(tree.getElement().getPrefix(), padded)))
                .withAfter(spaced(tree.getAfter(), padded)));
        if (isElseIf(getCursor().getParentTreeCursor().getValue())) {
            // the if of elseif is spelled as nothing, so the space after elseif is the if's own
            return c.withPrefix(spaced(c.getPrefix(), false));
        }
        return c.withPrefix(spaced(c.getPrefix(), style.getBeforeParentheses().getKeywords()));
    }

    /**
     * Whether an if is the one Lucee's {@code elseif} holds, whose keyword is spelled as nothing.
     */
    private static boolean isElseIf(Object tree) {
        return tree instanceof J.If && ((J.If) tree).getMarkers().findFirst(KeywordSpelling.class)
                .map(spelling -> spelling.getText().isEmpty()).orElse(false);
    }

    @Override
    public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
        J.ForLoop.Control c = super.visitForControl(control, p);
        if (keepsSpacing()) {
            return c;
        }
        boolean padded = style.getWithin().getKeywordParentheses();
        boolean beforeSemicolon = style.getOther().getBeforeForSemicolon();
        boolean afterSemicolon = style.getOther().getAfterForSemicolon();
        int lastInit = c.getPadding().getInit().size() - 1;
        List<JRightPadded<Statement>> init = ListUtils.map(c.getPadding().getInit(), (i, e) -> {
            J element = e.getElement();
            if (element instanceof J.Empty) {
                return e;
            }
            Space prefix = spaced(element.getPrefix(), i == 0 ? padded : style.getOther().getAfterComma());
            return e.withElement(element.withPrefix(prefix)).withAfter(spaced(e.getAfter(),
                    i == lastInit ? beforeSemicolon : style.getOther().getBeforeComma()));
        });
        JRightPadded<Expression> condition = c.getPadding().getCondition();
        if (!(condition.getElement() instanceof J.Empty)) {
            condition = condition.withElement(condition.getElement().withPrefix(spaced(condition.getElement().getPrefix(), afterSemicolon)))
                    .withAfter(spaced(condition.getAfter(), beforeSemicolon));
        }
        int lastUpdate = c.getPadding().getUpdate().size() - 1;
        List<JRightPadded<Statement>> update = ListUtils.map(c.getPadding().getUpdate(), (i, e) -> {
            J element = e.getElement();
            if (element instanceof J.Empty) {
                return e;
            }
            Space prefix = spaced(element.getPrefix(), i == 0 ? afterSemicolon : style.getOther().getAfterComma());
            return e.withElement(element.withPrefix(prefix)).withAfter(spaced(e.getAfter(),
                    i == lastUpdate ? padded : style.getOther().getBeforeComma()));
        });
        c = c.getPadding().withInit(init).getPadding().withCondition(condition).getPadding().withUpdate(update);
        return c.withPrefix(spaced(c.getPrefix(), style.getBeforeParentheses().getKeywords()));
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = super.visitForEachControl(control, p);
        if (keepsSpacing()) {
            return c;
        }
        boolean padded = style.getWithin().getKeywordParentheses();
        JRightPadded<Statement> variable = c.getPadding().getVariable();
        variable = variable.withElement(variable.getElement().withPrefix(spaced(variable.getElement().getPrefix(), padded)))
                .withAfter(single(variable.getAfter()));
        JRightPadded<Expression> iterable = c.getPadding().getIterable();
        iterable = iterable.withElement(iterable.getElement().withPrefix(single(iterable.getElement().getPrefix())))
                .withAfter(spaced(iterable.getAfter(), padded));
        c = c.getPadding().withVariable(variable).getPadding().withIterable(iterable);
        return c.withPrefix(spaced(c.getPrefix(), style.getBeforeParentheses().getKeywords()));
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);
        if (keepsSpacing()) {
            return i;
        }
        return i.getPadding().withThenPart(braced(i.getPadding().getThenPart(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.If.Else visitElse(J.If.Else anElse, P p) {
        J.If.Else e = super.visitElse(anElse, p);
        if (keepsSpacing()) {
            return e;
        }
        J.If owner = getCursor().getParentTreeCursor().getValue();
        if (owner.getThenPart() instanceof J.Block) {
            e = e.withPrefix(spaced(e.getPrefix(), style.getOther().getBeforeKeywordAfterBlock()));
        }
        JRightPadded<Statement> body = e.getPadding().getBody();
        if (body.getElement() instanceof J.If) {
            Space prefix = body.getElement().getPrefix();
            return e.getPadding().withBody(body.withElement(body.getElement().withPrefix(isElseIf(body.getElement()) ?
                    spaced(prefix, style.getBeforeParentheses().getKeywords()) : single(prefix))));
        }
        return e.getPadding().withBody(braced(body, style.getOther().getBeforeKeywordBrace()));
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        return keepsSpacing() ? w : w.getPadding().withBody(braced(w.getPadding().getBody(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = super.visitForLoop(forLoop, p);
        return keepsSpacing() ? f : f.getPadding().withBody(braced(f.getPadding().getBody(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        return keepsSpacing() ? f : f.getPadding().withBody(braced(f.getPadding().getBody(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        if (keepsSpacing()) {
            return d;
        }
        d = d.getPadding().withBody(braced(d.getPadding().getBody(), style.getOther().getBeforeKeywordBrace()));
        JLeftPadded<J.ControlParentheses<Expression>> condition = d.getPadding().getWhileCondition();
        if (d.getBody() instanceof J.Block) {
            condition = condition.withBefore(spaced(condition.getBefore(), style.getOther().getBeforeKeywordAfterBlock()));
        }
        return d.getPadding().withWhileCondition(condition);
    }

    @Override
    public J.Switch visitSwitch(J.Switch aSwitch, P p) {
        J.Switch s = super.visitSwitch(aSwitch, p);
        return keepsSpacing() ? s : s.withCases(brace(s.getCases(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.Case visitCase(J.Case aCase, P p) {
        J.Case c = super.visitCase(aCase, p);
        if (keepsSpacing()) {
            return c;
        }
        J label = c.getCaseLabels().get(0);
        if (!(label instanceof J.Identifier && "default".equalsIgnoreCase(((J.Identifier) label).getSimpleName()))) {
            c = c.withCaseLabels(ListUtils.mapFirst(c.getCaseLabels(), l -> l.withPrefix(single(l.getPrefix()))));
        }
        JContainer<Statement> statements = c.getPadding().getStatements();
        return c.getPadding().withStatements(statements.withBefore(spaced(statements.getBefore(), false)));
    }

    @Override
    public J.Try visitTry(J.Try tryable, P p) {
        J.Try t = super.visitTry(tryable, p);
        if (keepsSpacing()) {
            return t;
        }
        t = t.withBody(brace(t.getBody(), style.getOther().getBeforeKeywordBrace()));
        JLeftPadded<J.Block> finallyBlock = t.getPadding().getFinally();
        if (finallyBlock != null) {
            t = t.getPadding().withFinally(finallyBlock.withBefore(spaced(finallyBlock.getBefore(), style.getOther().getBeforeKeywordAfterBlock()))
                    .withElement(brace(finallyBlock.getElement(), style.getOther().getBeforeKeywordBrace())));
        }
        return t;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch aCatch, P p) {
        J.Try.Catch c = super.visitCatch(aCatch, p);
        if (keepsSpacing()) {
            return c;
        }
        return c.withPrefix(spaced(c.getPrefix(), style.getOther().getBeforeKeywordAfterBlock())).withBody(brace(c.getBody(), style.getOther().getBeforeStatementBrace()));
    }

    @Override
    public J.Return visitReturn(J.Return aReturn, P p) {
        J.Return r = super.visitReturn(aReturn, p);
        if (keepsSpacing() || r.getExpression() == null) {
            return r;
        }
        return r.withExpression(r.getExpression().withPrefix(single(r.getExpression().getPrefix())));
    }

    @Override
    public J.Throw visitThrow(J.Throw thrown, P p) {
        J.Throw t = super.visitThrow(thrown, p);
        if (keepsSpacing()) {
            return t;
        }
        return t.withException(t.getException().withPrefix(single(t.getException().getPrefix())));
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        if (keepsSpacing()) {
            return b;
        }
        return b.getPadding().withStatements(beforeSemicolons(b.getPadding().getStatements()));
    }

    @Override
    public Cf.CompilationUnit visitCompilationUnit(Cf.CompilationUnit compilationUnit, P p) {
        Cf.CompilationUnit c = super.visitCompilationUnit(compilationUnit, p);
        if (keepsSpacing()) {
            return c;
        }
        return c.getPadding().withStatements(beforeSemicolons(c.getPadding().getStatements()));
    }

    private List<JRightPadded<Statement>> beforeSemicolons(List<JRightPadded<Statement>> statements) {
        return ListUtils.map(statements, s -> s.getMarkers().findFirst(Semicolon.class).isPresent() ?
                s.withAfter(spaced(s.getAfter(), false)) : s);
    }

    // ------------------------------------------------------------------------------------------
    // Declarations
    // ------------------------------------------------------------------------------------------

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (keepsSpacing()) {
            return c;
        }
        boolean padded = attributeEquals(c);
        c = c.withLeadingAnnotations(attributes(c.getLeadingAnnotations(), padded));
        c = c.withMarkers(clauses(c.getMarkers(), padded));
        JLeftPadded<TypeTree> extendings = c.getPadding().getExtends();
        if (extendings != null) {
            c = c.getPadding().withExtends(extendings.withBefore(single(extendings.getBefore()))
                    .withMarkers(clauses(extendings.getMarkers(), padded)));
        }
        JContainer<TypeTree> implementings = c.getPadding().getImplements();
        if (implementings != null) {
            c = c.getPadding().withImplements(implementings.withBefore(single(implementings.getBefore()))
                    .withMarkers(clauses(implementings.getMarkers(), padded)));
        }
        if (TemplateOutput.isTagSyntax(c)) {
            return c;
        }
        J.ClassDeclaration.Kind kind = c.getPadding().getKind();
        if (!c.getModifiers().isEmpty()) {
            c = c.getPadding().withKind(kind.withPrefix(single(kind.getPrefix())));
        }
        c = c.withModifiers(ListUtils.map(c.getModifiers(), (i, modifier) -> i == 0 ? modifier : modifier.withPrefix(single(modifier.getPrefix()))));
        return c.withBody(brace(c.getBody(), style.getOther().getBeforeComponentBrace()));
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);
        if (keepsSpacing()) {
            return v;
        }
        boolean padded = attributeEquals(v);
        v = v.withLeadingAnnotations(attributes(v.getLeadingAnnotations(), padded));
        v = v.withMarkers(clauses(v.getMarkers(), padded));
        if (TemplateOutput.isTagSyntax(v)) {
            return v;
        }
        v = v.withModifiers(ListUtils.map(v.getModifiers(), (i, modifier) -> i == 0 ? modifier : modifier.withPrefix(single(modifier.getPrefix()))));
        if (v.getTypeExpression() != null && !v.getModifiers().isEmpty() && !hasClause(v.getMarkers(), "type")) {
            v = v.withTypeExpression(v.getTypeExpression().withPrefix(single(v.getTypeExpression().getPrefix())));
        }
        if ((v.getTypeExpression() != null && !hasClause(v.getMarkers(), "type") || !v.getModifiers().isEmpty()) &&
            !hasClause(v.getMarkers(), "name")) {
            v = v.getPadding().withVariables(ListUtils.mapFirst(v.getPadding().getVariables(), variable -> isAligned(variable.getElement().getPrefix()) ?
                    variable : variable.withElement(variable.getElement().withPrefix(single(variable.getElement().getPrefix())))));
        }
        return v;
    }

    @Override
    public Cf.Closure visitClosure(Cf.Closure closure, P p) {
        Cf.Closure c = super.visitClosure(closure, p);
        return keepsSpacing() ? c : c.withAttributes(attributes(c.getAttributes(), style.getOther().getAroundMetadataEquals()));
    }

    /**
     * A declaration's attributes, {@code output=false}, each after a single space.
     */
    private List<J.Annotation> attributes(List<J.Annotation> annotations, boolean padded) {
        return ListUtils.map(annotations, a -> {
            J.Annotation annotation = a.withPrefix(attributePrefix(a.getPrefix()));
            JContainer<Expression> arguments = annotation.getPadding().getArguments();
            if (arguments == null) {
                return annotation;
            }
            arguments = arguments.withBefore(spaced(arguments.getBefore(), padded));
            arguments = arguments.getPadding().withElements(ListUtils.mapFirst(arguments.getPadding().getElements(), e ->
                    e.withElement(e.getElement().withPrefix(spaced(e.getElement().getPrefix(), padded)))));
            return annotation.getPadding().withArguments(arguments);
        });
    }

    /**
     * The attributes a declaration holds as its parts, {@code name="init"}, and the end of its tag when it is
     * written as a tag.
     */
    private Markers clauses(Markers markers, boolean padded) {
        return markers.withMarkers(ListUtils.map(markers.getMarkers(), marker -> {
            if (marker instanceof AttributeClause) {
                AttributeClause clause = (AttributeClause) marker;
                return clause.withPrefix(clause.getPrefix().isEmpty() ? clause.getPrefix() : attributePrefix(clause.getPrefix()))
                        .withBeforeEquals(spaced(clause.getBeforeEquals(), padded))
                        .withAfterEquals(spaced(clause.getAfterEquals(), padded));
            } else if (marker instanceof TagSyntax) {
                TagSyntax tag = (TagSyntax) marker;
                tag = tag.withBeforeEnd(spaced(tag.getBeforeEnd(), tag.isSelfClosing() ?
                        style.getTags().getBeforeSelfClosingEnd() : style.getTags().getBeforeTagEnd()));
                Cf.Tag.Closing closing = tag.getClosing();
                return closing == null ? tag : tag.withClosing(closing.withBeforeEnd(spaced(closing.getBeforeEnd(), style.getTags().getBeforeTagEnd())));
            }
            return marker;
        }));
    }

    /**
     * Whether the {@code =} of a declaration's attributes has a space on each side: as a tag's when it is written
     * as a tag, otherwise as script's metadata.
     */
    private boolean attributeEquals(J declaration) {
        return TemplateOutput.isTagSyntax(declaration) ? style.getTags().getAroundAttributeEquals() :
                style.getOther().getAroundMetadataEquals();
    }

    private static boolean hasClause(Markers markers, String name) {
        return markers.getMarkers().stream().anyMatch(m -> m instanceof AttributeClause && ((AttributeClause) m).isNamed(name));
    }

    // ------------------------------------------------------------------------------------------
    // Tags
    // ------------------------------------------------------------------------------------------

    @Override
    public Cf.Tag visitTag(Cf.Tag tag, P p) {
        Cf.Tag t = super.visitTag(tag, p);
        if (keepsSpacing()) {
            return t;
        }
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> a.withPrefix(attributePrefix(a.getPrefix()))));
        if (t.getExpression() != null) {
            t = t.withExpression(t.getExpression().withPrefix(single(t.getExpression().getPrefix())));
        }
        t = t.withBeforeEnd(spaced(t.getBeforeEnd(), t.isSelfClosing() ?
                style.getTags().getBeforeSelfClosingEnd() : style.getTags().getBeforeTagEnd()));
        Cf.Tag.Closing closing = t.getClosing();
        return closing == null ? t : t.withClosing(closing.withBeforeEnd(spaced(closing.getBeforeEnd(), style.getTags().getBeforeTagEnd())));
    }

    @Override
    public Cf.TagAttribute visitTagAttribute(Cf.TagAttribute attribute, P p) {
        Cf.TagAttribute a = super.visitTagAttribute(attribute, p);
        JLeftPadded<Expression> value = a.getPadding().getValue();
        if (keepsSpacing() || value == null) {
            return a;
        }
        boolean padded = getCursor().getParentTreeCursor().getValue() instanceof Cf.Tag ?
                style.getTags().getAroundAttributeEquals() : style.getOther().getAroundMetadataEquals();
        return a.getPadding().withValue(value.withBefore(spaced(value.getBefore(), padded))
                .withElement(value.getElement().withPrefix(spaced(value.getElement().getPrefix(), padded))));
    }

    @Override
    public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, P p) {
        Cf.ScriptTag s = super.visitScriptTag(scriptTag, p);
        if (keepsSpacing()) {
            return s;
        }
        s = s.withAttributes(ListUtils.map(s.getAttributes(), a -> a.withPrefix(attributePrefix(a.getPrefix()))));
        if (s.getExpression() != null) {
            s = s.withExpression(s.getExpression().withPrefix(single(s.getExpression().getPrefix())));
        }
        return s.getBody() == null ? s : s.withBody(brace(s.getBody(), style.getOther().getBeforeComponentBrace()));
    }

    @Override
    public Cf.TagCall visitTagCall(Cf.TagCall tagCall, P p) {
        Cf.TagCall t = super.visitTagCall(tagCall, p);
        return keepsSpacing() ? t : t.withBody(brace(t.getBody(), style.getOther().getBeforeStatementBrace()));
    }

    // ------------------------------------------------------------------------------------------
    // Spaces
    // ------------------------------------------------------------------------------------------

    /**
     * A block's opening brace after one space or none.
     */
    private static J.Block brace(J.Block block, boolean spaced) {
        return block.withPrefix(spaced(block.getPrefix(), spaced));
    }

    private static JRightPadded<Statement> braced(JRightPadded<Statement> body, boolean spaced) {
        return body.getElement() instanceof J.Block ? body.withElement(brace((J.Block) body.getElement(), spaced)) : body;
    }

    /**
     * A list in parentheses, brackets or braces: padded within or not, and a comma followed by a space or not.
     */
    private <T extends J> List<JRightPadded<T>> delimited(List<JRightPadded<T>> elements, boolean within, boolean emptyWithin) {
        if (elements.size() == 1 && elements.get(0).getElement() instanceof J.Empty) {
            return ListUtils.mapFirst(elements, e -> e.withElement(e.getElement().withPrefix(spaced(e.getElement().getPrefix(), emptyWithin)))
                    .withAfter(spaced(e.getAfter(), false)));
        }
        int last = elements.size() - 1;
        return ListUtils.map(elements, (i, e) -> {
            J element = e.getElement();
            // a comma that starts a line, as in a leading-comma list, keeps the space after it
            boolean comma = i > 0 && !elements.get(i - 1).getMarkers().findFirst(OmitComma.class).isPresent() &&
                            !Layout.hasLineBreak(elements.get(i - 1).getAfter());
            if (i == 0) {
                element = element.withPrefix(spaced(element.getPrefix(), within));
            } else if (comma) {
                element = element.withPrefix(spaced(element.getPrefix(), style.getOther().getAfterComma()));
            }
            Space after = e.getAfter();
            Markers markers = e.getMarkers();
            if (i < last) {
                if (!markers.findFirst(OmitComma.class).isPresent()) {
                    after = spaced(after, style.getOther().getBeforeComma());
                }
            } else {
                TrailingComma trailingComma = markers.findFirst(TrailingComma.class).orElse(null);
                if (trailingComma != null) {
                    after = spaced(after, style.getOther().getBeforeComma());
                    markers = markers.setByType(trailingComma.withSuffix(spaced(trailingComma.getSuffix(), within)));
                } else {
                    after = spaced(after, within);
                }
            }
            //noinspection unchecked
            return e.withElement((T) element).withAfter(after).withMarkers(markers);
        });
    }

    /**
     * One space or none, unless the space holds a line break or a comment.
     */
    private static Space spaced(Space space, boolean on) {
        if (!space.getComments().isEmpty() || Layout.hasLineBreak(space.getWhitespace())) {
            return space;
        }
        if (on) {
            return " ".equals(space.getWhitespace()) ? space : Space.SINGLE_SPACE;
        }
        return space.getWhitespace().isEmpty() ? space : Space.EMPTY;
    }

    /**
     * One space or none between two tokens. A space is kept where they would run together without one, but none is
     * added where there was none, since tokens written together already lex apart.
     */
    private Space spaced(Space space, boolean on, char before, char after) {
        if (!on && !Tokens.canJoin(before, after)) {
            return single(space);
        }
        return spaced(space, on);
    }

    /**
     * Whether the space before an assignment's operator, a struct key's separator or a parameter's name lines it up
     * with those around it, which the style keeps. Both sides of the operator are then left as written.
     */
    private boolean isAligned(Space space) {
        return style.getOther().getKeepAlignedAssignments() && isRun(space);
    }

    /**
     * The space before an attribute: a single space, or the spaces that line it up with the attributes around it
     * where the style keeps alignment.
     */
    private Space attributePrefix(Space space) {
        if (style.getTags().getKeepAlignedAttributes() && isRun(space)) {
            return space;
        }
        return single(space);
    }

    private static boolean isRun(Space space) {
        String whitespace = space.getWhitespace();
        return space.getComments().isEmpty() && whitespace.length() > 1 && !Layout.hasLineBreak(whitespace);
    }

    /**
     * A single space where there are spaces, so spaces after a keyword or between attributes collapse to one.
     */
    private static Space single(Space space) {
        return space.getWhitespace().isEmpty() ? space : spaced(space, true);
    }
}
