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
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.marker.AttributeClause;
import org.openrewrite.coldfusion.marker.FunctionKeyword;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.VarKeyword;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

/**
 * Puts a space where a tree built without whitespace would otherwise print tokens that run together. CFML's
 * keywords double as names, so {@code var x} printed as {@code varx} would be a name, and a spelled operator needs
 * spaces, as {@code a EQ b} does. Only an empty space between two tokens that would join is changed, so a tree the
 * parser built stays as it is.
 */
public class MinimumViableSpacingVisitor<P> extends FormattingVisitor<P> {

    public MinimumViableSpacingVisitor() {
        this(null);
    }

    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        super(stopAfter);
    }

    private boolean stopped() {
        return getCursor().getNearestMessage("stop") != null;
    }

    @Override
    public J.Return visitReturn(J.Return aReturn, P p) {
        J.Return r = super.visitReturn(aReturn, p);
        Expression e = r.getExpression();
        return e == null || stopped() ? r : r.withExpression(after(keyword(r.getMarkers(), "return"), e));
    }

    @Override
    public J.Throw visitThrow(J.Throw thrown, P p) {
        J.Throw t = super.visitThrow(thrown, p);
        return stopped() ? t : t.withException(after(keyword(t.getMarkers(), "throw"), t.getException()));
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = super.visitNewClass(newClass, p);
        TypeTree clazz = n.getClazz();
        return clazz == null || stopped() ? n : n.withClazz(after(keyword(n.getMarkers(), "new"), clazz));
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        if (stopped()) {
            return a;
        }
        VarKeyword var = a.getMarkers().findFirst(VarKeyword.class).orElse(null);
        if (var != null) {
            a = a.withVariable(after(var.getKeyword(), a.getVariable()));
        }
        return a.withAssignment(after("=", a.getAssignment()));
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        if (stopped()) {
            return a;
        }
        VarKeyword var = a.getMarkers().findFirst(VarKeyword.class).orElse(null);
        if (var != null) {
            a = a.withVariable(after(var.getKeyword(), a.getVariable()));
        }
        return a.withAssignment(after("=", a.getAssignment()));
    }

    @Override
    public Cf.AssignmentOperation visitAssignmentOperation(Cf.AssignmentOperation assignOp, P p) {
        Cf.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        if (stopped()) {
            return a;
        }
        VarKeyword var = a.getMarkers().findFirst(VarKeyword.class).orElse(null);
        if (var != null) {
            a = a.withVariable(after(var.getKeyword(), a.getVariable()));
        }
        return a.withAssignment(after("=", a.getAssignment()));
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        if (stopped()) {
            return v;
        }
        VarKeyword var = v.getMarkers().findFirst(VarKeyword.class).orElse(null);
        if (var != null) {
            v = v.withName(after(var.getKeyword(), v.getName()));
        }
        JLeftPadded<Expression> initializer = v.getPadding().getInitializer();
        if (initializer != null) {
            v = v.getPadding().withInitializer(initializer.withElement(after("=", initializer.getElement())));
        }
        return v;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary b = super.visitBinary(binary, p);
        if (stopped()) {
            return b;
        }
        String operator = Operators.binary(b);
        b = b.getPadding().withOperator(b.getPadding().getOperator().withBefore(
                between(b.getPadding().getOperator().getBefore(), Tokens.last(b.getLeft(), getCursor()), operator.charAt(0))));
        return b.withRight(after(operator, b.getRight()));
    }

    @Override
    public Cf.Binary visitBinary(Cf.Binary binary, P p) {
        Cf.Binary b = super.visitBinary(binary, p);
        if (stopped()) {
            return b;
        }
        String operator = Operators.binary(b);
        b = b.getPadding().withOperator(b.getPadding().getOperator().withBefore(
                between(b.getPadding().getOperator().getBefore(), Tokens.last(b.getLeft(), getCursor()), operator.charAt(0))));
        return b.withRight(after(operator, b.getRight()));
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        if (stopped() || u.getOperator() == J.Unary.Type.PostIncrement || u.getOperator() == J.Unary.Type.PostDecrement) {
            return u;
        }
        return u.withExpression(after(Operators.unary(u), u.getExpression()));
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = super.visitTernary(ternary, p);
        if (stopped()) {
            return t;
        }
        JLeftPadded<Expression> truePart = t.getPadding().getTruePart();
        truePart = truePart.withBefore(between(truePart.getBefore(), Tokens.last(t.getCondition(), getCursor()), '?'))
                .withElement(after("?", truePart.getElement()));
        JLeftPadded<Expression> falsePart = t.getPadding().getFalsePart();
        falsePart = falsePart.withBefore(between(falsePart.getBefore(), Tokens.last(truePart.getElement(), getCursor()), ':'))
                .withElement(after(":", falsePart.getElement()));
        return t.getPadding().withTruePart(truePart).getPadding().withFalsePart(falsePart);
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = super.visitForEachControl(control, p);
        if (stopped()) {
            return c;
        }
        String in = keyword(c.getMarkers(), "in");
        JRightPadded<Statement> variable = c.getPadding().getVariable();
        c = c.getPadding().withVariable(variable.withAfter(between(variable.getAfter(), Tokens.last(variable.getElement(), getCursor()), in.charAt(0))));
        JRightPadded<Expression> iterable = c.getPadding().getIterable();
        return c.getPadding().withIterable(iterable.withElement(after(in, iterable.getElement())));
    }

    @Override
    public J.Case visitCase(J.Case aCase, P p) {
        J.Case c = super.visitCase(aCase, p);
        J label = c.getCaseLabels().get(0);
        if (stopped() || label instanceof J.Identifier && "default".equalsIgnoreCase(((J.Identifier) label).getSimpleName())) {
            return c;
        }
        String keyword = keyword(c.getMarkers(), "case");
        return c.withCaseLabels(ListUtils.mapFirst(c.getCaseLabels(), l -> after(keyword, l)));
    }

    @Override
    public J.If.Else visitElse(J.If.Else anElse, P p) {
        J.If.Else e = super.visitElse(anElse, p);
        if (stopped()) {
            return e;
        }
        String keyword = keyword(e.getMarkers(), "else");
        JRightPadded<Statement> body = e.getPadding().getBody();
        return e.getPadding().withBody(body.withElement(after(keyword, body.getElement())));
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        if (stopped()) {
            return m;
        }
        m = m.withLeadingAnnotations(separated(m.getLeadingAnnotations()));
        m = m.withMarkers(separated(m.getMarkers()));
        if (TemplateOutput.isTagSyntax(m)) {
            return m;
        }
        m = m.withModifiers(modifiers(m.getModifiers()));
        boolean wordBefore = !m.getModifiers().isEmpty();
        TypeTree returnType = m.getReturnTypeExpression();
        if (returnType != null && !hasClause(m.getMarkers(), "returntype")) {
            if (wordBefore) {
                m = m.withReturnTypeExpression(atLeastOne(returnType));
            }
            wordBefore = true;
        }
        FunctionKeyword keyword = m.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
        if (keyword != null && wordBefore && keyword.getPrefix().isEmpty()) {
            m = m.withMarkers(m.getMarkers().setByType(keyword.withPrefix(Space.SINGLE_SPACE)));
        }
        return m.withName(atLeastOne(m.getName()));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (stopped()) {
            return c;
        }
        c = c.withLeadingAnnotations(separated(c.getLeadingAnnotations()));
        c = c.withMarkers(separated(c.getMarkers()));
        JLeftPadded<TypeTree> extendings = c.getPadding().getExtends();
        if (extendings != null && extendings.getBefore().isEmpty()) {
            c = c.getPadding().withExtends(extendings.withBefore(Space.SINGLE_SPACE));
        }
        JContainer<TypeTree> implementings = c.getPadding().getImplements();
        if (implementings != null && implementings.getBefore().isEmpty()) {
            c = c.getPadding().withImplements(implementings.withBefore(Space.SINGLE_SPACE));
        }
        if (TemplateOutput.isTagSyntax(c)) {
            return c;
        }
        c = c.withModifiers(modifiers(c.getModifiers()));
        J.ClassDeclaration.Kind kind = c.getPadding().getKind();
        if (!c.getModifiers().isEmpty() && kind.getPrefix().isEmpty()) {
            c = c.getPadding().withKind(kind.withPrefix(Space.SINGLE_SPACE));
        }
        return c;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);
        if (stopped()) {
            return v;
        }
        v = v.withLeadingAnnotations(separated(v.getLeadingAnnotations()));
        v = v.withMarkers(separated(v.getMarkers()));
        if (TemplateOutput.isTagSyntax(v)) {
            return v;
        }
        v = v.withModifiers(modifiers(v.getModifiers()));
        boolean wordBefore = !v.getModifiers().isEmpty();
        TypeTree type = v.getTypeExpression();
        if (type != null && !hasClause(v.getMarkers(), "type")) {
            if (wordBefore) {
                v = v.withTypeExpression(atLeastOne(type));
            }
            wordBefore = true;
        }
        if (wordBefore && !hasClause(v.getMarkers(), "name")) {
            v = v.getPadding().withVariables(ListUtils.mapFirst(v.getPadding().getVariables(), variable ->
                    variable.withElement(atLeastOne(variable.getElement()))));
        }
        return v;
    }

    @Override
    public Cf.Closure visitClosure(Cf.Closure closure, P p) {
        Cf.Closure c = super.visitClosure(closure, p);
        return stopped() ? c : c.withAttributes(separated(c.getAttributes()));
    }

    @Override
    public Cf.Tag visitTag(Cf.Tag tag, P p) {
        Cf.Tag t = super.visitTag(tag, p);
        if (stopped()) {
            return t;
        }
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> a.getPrefix().isEmpty() ? a.withPrefix(Space.SINGLE_SPACE) : a));
        J expression = t.getExpression();
        if (expression != null && expression.getPrefix().isEmpty() &&
            !Tokens.canJoin(t.getName(), Tokens.first(expression, getCursor()))) {
            t = t.withExpression(expression.withPrefix(Space.SINGLE_SPACE));
        }
        return t;
    }

    @Override
    public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, P p) {
        Cf.ScriptTag s = super.visitScriptTag(scriptTag, p);
        if (stopped()) {
            return s;
        }
        s = s.withAttributes(ListUtils.map(s.getAttributes(), a -> a.getPrefix().isEmpty() ? a.withPrefix(Space.SINGLE_SPACE) : a));
        Expression expression = s.getExpression();
        return expression == null ? s : s.withExpression(after(s.getName().getSimpleName(), expression));
    }

    /**
     * Modifiers after the first, each after a space.
     */
    private List<J.Modifier> modifiers(List<J.Modifier> modifiers) {
        return ListUtils.map(modifiers, (i, m) -> i == 0 || !m.getPrefix().isEmpty() ? m : m.withPrefix(Space.SINGLE_SPACE));
    }

    /**
     * A declaration's attributes, each after a space.
     */
    private List<J.Annotation> separated(List<J.Annotation> annotations) {
        return ListUtils.map(annotations, a -> a.getPrefix().isEmpty() ? a.withPrefix(Space.SINGLE_SPACE) : a);
    }

    private Markers separated(Markers markers) {
        return markers.withMarkers(ListUtils.map(markers.getMarkers(), marker -> {
            if (marker instanceof AttributeClause) {
                AttributeClause clause = (AttributeClause) marker;
                // a component's extends and implements keep the space before them in their padding
                boolean supertype = clause.isNamed("extends") || clause.isNamed("implements");
                return supertype || !clause.getPrefix().isEmpty() ? clause : clause.withPrefix(Space.SINGLE_SPACE);
            }
            return marker;
        }));
    }

    private static boolean hasClause(Markers markers, String name) {
        return markers.getMarkers().stream().anyMatch(m -> m instanceof AttributeClause && ((AttributeClause) m).isNamed(name));
    }

    /**
     * The element after a token, with a space between them when they would otherwise run together. A word is kept
     * apart from a string or an interpolation after it too, as {@code return "x"} and {@code CONTAINS "x"} are written.
     */
    private <J2 extends J> J2 after(String token, J2 element) {
        if (!element.getPrefix().isEmpty()) {
            return element;
        }
        char first = Tokens.first(element, getCursor());
        boolean wordThenQuote = !token.isEmpty() && Tokens.isWordChar(token.charAt(token.length() - 1)) &&
                                (first == '"' || first == '\'' || first == '#');
        if (!wordThenQuote && Tokens.canJoin(token, first)) {
            return element;
        }
        return element.withPrefix(Space.SINGLE_SPACE);
    }

    private static Space between(Space space, char before, char after) {
        return space.isEmpty() && !Tokens.canJoin(before, after) ? Space.SINGLE_SPACE : space;
    }

    private static <J2 extends J> J2 atLeastOne(J2 element) {
        return element.getPrefix().isEmpty() ? element.withPrefix(Space.SINGLE_SPACE) : element;
    }

    private static String keyword(Markers markers, String canonical) {
        return markers.findFirst(KeywordSpelling.class).map(KeywordSpelling::getText).orElse(canonical);
    }
}
