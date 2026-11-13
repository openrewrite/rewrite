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
import org.openrewrite.coldfusion.marker.VarKeyword;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves whitespace to the outermost element that starts where it ends, as the parser places it. A recipe that
 * removes a function's {@code public} leaves the space before its return type, and one that builds an expression
 * may give its first operand the space before it. This gives that space to the function or the expression, where
 * the other formatting visitors look for it.
 */
public class NormalizeFormatVisitor<P> extends FormattingVisitor<P> {

    public NormalizeFormatVisitor() {
        this(null);
    }

    public NormalizeFormatVisitor(@Nullable Tree stopAfter) {
        super(stopAfter);
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        J j = hoist(tree);
        return super.postVisit(j, p);
    }

    private J hoist(J j) {
        if (j instanceof J.Binary) {
            J.Binary b = (J.Binary) j;
            return b.getLeft().getPrefix().isEmpty() ? b : b.withPrefix(concat(b.getPrefix(), b.getLeft().getPrefix()))
                    .withLeft(b.getLeft().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.Binary) {
            Cf.Binary b = (Cf.Binary) j;
            return b.getLeft().getPrefix().isEmpty() ? b : b.withPrefix(concat(b.getPrefix(), b.getLeft().getPrefix()))
                    .withLeft(b.getLeft().withPrefix(Space.EMPTY));
        } else if (j instanceof J.Assignment && !hasVar(j)) {
            J.Assignment a = (J.Assignment) j;
            return a.getVariable().getPrefix().isEmpty() ? a : a.withPrefix(concat(a.getPrefix(), a.getVariable().getPrefix()))
                    .withVariable(a.getVariable().withPrefix(Space.EMPTY));
        } else if (j instanceof J.AssignmentOperation && !hasVar(j)) {
            J.AssignmentOperation a = (J.AssignmentOperation) j;
            return a.getVariable().getPrefix().isEmpty() ? a : a.withPrefix(concat(a.getPrefix(), a.getVariable().getPrefix()))
                    .withVariable(a.getVariable().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.AssignmentOperation && !hasVar(j)) {
            Cf.AssignmentOperation a = (Cf.AssignmentOperation) j;
            return a.getVariable().getPrefix().isEmpty() ? a : a.withPrefix(concat(a.getPrefix(), a.getVariable().getPrefix()))
                    .withVariable(a.getVariable().withPrefix(Space.EMPTY));
        } else if (j instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) j;
            Expression select = m.getSelect();
            if (select != null) {
                return select.getPrefix().isEmpty() ? m : m.withPrefix(concat(m.getPrefix(), select.getPrefix()))
                        .withSelect(select.withPrefix(Space.EMPTY));
            }
            return m.getName().getPrefix().isEmpty() ? m : m.withPrefix(concat(m.getPrefix(), m.getName().getPrefix()))
                    .withName(m.getName().withPrefix(Space.EMPTY));
        } else if (j instanceof J.FieldAccess) {
            J.FieldAccess f = (J.FieldAccess) j;
            return f.getTarget().getPrefix().isEmpty() ? f : f.withPrefix(concat(f.getPrefix(), f.getTarget().getPrefix()))
                    .withTarget(f.getTarget().withPrefix(Space.EMPTY));
        } else if (j instanceof J.ArrayAccess) {
            J.ArrayAccess a = (J.ArrayAccess) j;
            return a.getIndexed().getPrefix().isEmpty() ? a : a.withPrefix(concat(a.getPrefix(), a.getIndexed().getPrefix()))
                    .withIndexed(a.getIndexed().withPrefix(Space.EMPTY));
        } else if (j instanceof J.Ternary) {
            J.Ternary t = (J.Ternary) j;
            return t.getCondition().getPrefix().isEmpty() ? t : t.withPrefix(concat(t.getPrefix(), t.getCondition().getPrefix()))
                    .withCondition(t.getCondition().withPrefix(Space.EMPTY));
        } else if (j instanceof J.Unary) {
            J.Unary u = (J.Unary) j;
            boolean postfix = u.getOperator() == J.Unary.Type.PostIncrement || u.getOperator() == J.Unary.Type.PostDecrement;
            return !postfix || u.getExpression().getPrefix().isEmpty() ? u :
                    u.withPrefix(concat(u.getPrefix(), u.getExpression().getPrefix())).withExpression(u.getExpression().withPrefix(Space.EMPTY));
        } else if (j instanceof J.Lambda) {
            J.Lambda l = (J.Lambda) j;
            FunctionKeyword keyword = l.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
            if (keyword != null) {
                return keyword.getPrefix().isEmpty() ? l : l.withPrefix(concat(l.getPrefix(), keyword.getPrefix()))
                        .withMarkers(l.getMarkers().setByType(keyword.withPrefix(Space.EMPTY)));
            }
            return l.getParameters().getPrefix().isEmpty() ? l : l.withPrefix(concat(l.getPrefix(), l.getParameters().getPrefix()))
                    .withParameters(l.getParameters().withPrefix(Space.EMPTY));
        } else if (j instanceof J.MethodDeclaration && !TemplateOutput.isTagSyntax(j)) {
            return method((J.MethodDeclaration) j);
        } else if (j instanceof J.VariableDeclarations && !TemplateOutput.isTagSyntax(j)) {
            return variables((J.VariableDeclarations) j);
        } else if (j instanceof J.VariableDeclarations.NamedVariable && !hasVar(j)) {
            J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) j;
            return v.getName().getPrefix().isEmpty() ? v : v.withPrefix(concat(v.getPrefix(), v.getName().getPrefix()))
                    .withName(v.getName().withPrefix(Space.EMPTY));
        } else if (j instanceof J.ClassDeclaration && !TemplateOutput.isTagSyntax(j)) {
            return component((J.ClassDeclaration) j);
        } else if (j instanceof Cf.KeyValue) {
            Cf.KeyValue k = (Cf.KeyValue) j;
            return k.getKey().getPrefix().isEmpty() ? k : k.withPrefix(concat(k.getPrefix(), k.getKey().getPrefix()))
                    .withKey(k.getKey().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.Invocation) {
            Cf.Invocation i = (Cf.Invocation) j;
            return i.getTarget().getPrefix().isEmpty() ? i : i.withPrefix(concat(i.getPrefix(), i.getTarget().getPrefix()))
                    .withTarget(i.getTarget().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.TagCall) {
            Cf.TagCall t = (Cf.TagCall) j;
            return t.getCall().getPrefix().isEmpty() ? t : t.withPrefix(concat(t.getPrefix(), t.getCall().getPrefix()))
                    .withCall(t.getCall().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.Closure) {
            Cf.Closure c = (Cf.Closure) j;
            return c.getLambda().getPrefix().isEmpty() ? c : c.withPrefix(concat(c.getPrefix(), c.getLambda().getPrefix()))
                    .withLambda(c.getLambda().withPrefix(Space.EMPTY));
        } else if (j instanceof Cf.TypeLiteral) {
            Cf.TypeLiteral t = (Cf.TypeLiteral) j;
            return t.getName().getPrefix().isEmpty() ? t : t.withPrefix(concat(t.getPrefix(), t.getName().getPrefix()))
                    .withName(t.getName().withPrefix(Space.EMPTY));
        } else if (j instanceof J.NewArray && ((J.NewArray) j).getTypeExpression() == null) {
            J.NewArray n = (J.NewArray) j;
            JContainer<Expression> initializer = n.getPadding().getInitializer();
            return initializer == null || initializer.getBefore().isEmpty() ? n : n.withPrefix(concat(n.getPrefix(), initializer.getBefore()))
                    .getPadding().withInitializer(initializer.withBefore(Space.EMPTY));
        } else if (j instanceof Cf.StructLiteral) {
            Cf.StructLiteral s = (Cf.StructLiteral) j;
            JContainer<Expression> entries = s.getPadding().getEntries();
            return entries.getBefore().isEmpty() ? s : s.withPrefix(concat(s.getPrefix(), entries.getBefore()))
                    .getPadding().withEntries(entries.withBefore(Space.EMPTY));
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            return n.getNew().isEmpty() ? n : n.withPrefix(concat(n.getPrefix(), n.getNew())).withNew(Space.EMPTY);
        }
        return j;
    }

    private J.MethodDeclaration method(J.MethodDeclaration m) {
        if (!m.getModifiers().isEmpty()) {
            J.Modifier first = m.getModifiers().get(0);
            return first.getPrefix().isEmpty() ? m : m.withPrefix(concat(m.getPrefix(), first.getPrefix()))
                    .withModifiers(ListUtils.mapFirst(m.getModifiers(), mod -> mod.withPrefix(Space.EMPTY)));
        }
        TypeTree returnType = m.getReturnTypeExpression();
        if (returnType != null && !hasClause(m, "returntype")) {
            return returnType.getPrefix().isEmpty() ? m : m.withPrefix(concat(m.getPrefix(), returnType.getPrefix()))
                    .withReturnTypeExpression(returnType.withPrefix(Space.EMPTY));
        }
        FunctionKeyword keyword = m.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
        if (keyword != null) {
            return keyword.getPrefix().isEmpty() ? m : m.withPrefix(concat(m.getPrefix(), keyword.getPrefix()))
                    .withMarkers(m.getMarkers().setByType(keyword.withPrefix(Space.EMPTY)));
        }
        return m;
    }

    private J.VariableDeclarations variables(J.VariableDeclarations v) {
        if (!v.getModifiers().isEmpty()) {
            J.Modifier first = v.getModifiers().get(0);
            return first.getPrefix().isEmpty() ? v : v.withPrefix(concat(v.getPrefix(), first.getPrefix()))
                    .withModifiers(ListUtils.mapFirst(v.getModifiers(), mod -> mod.withPrefix(Space.EMPTY)));
        }
        TypeTree type = v.getTypeExpression();
        if (type != null && !hasClause(v, "type")) {
            return type.getPrefix().isEmpty() ? v : v.withPrefix(concat(v.getPrefix(), type.getPrefix()))
                    .withTypeExpression(type.withPrefix(Space.EMPTY));
        }
        if (hasClause(v, "name") || v.getVariables().isEmpty()) {
            return v;
        }
        J.VariableDeclarations.NamedVariable first = v.getVariables().get(0);
        return first.getPrefix().isEmpty() ? v : v.withPrefix(concat(v.getPrefix(), first.getPrefix()))
                .withVariables(ListUtils.mapFirst(v.getVariables(), variable -> variable.withPrefix(Space.EMPTY)));
    }

    private J.ClassDeclaration component(J.ClassDeclaration c) {
        if (!c.getModifiers().isEmpty()) {
            J.Modifier first = c.getModifiers().get(0);
            return first.getPrefix().isEmpty() ? c : c.withPrefix(concat(c.getPrefix(), first.getPrefix()))
                    .withModifiers(ListUtils.mapFirst(c.getModifiers(), mod -> mod.withPrefix(Space.EMPTY)));
        }
        J.ClassDeclaration.Kind kind = c.getPadding().getKind();
        return kind.getPrefix().isEmpty() ? c : c.withPrefix(concat(c.getPrefix(), kind.getPrefix()))
                .getPadding().withKind(kind.withPrefix(Space.EMPTY));
    }

    private static boolean hasVar(J j) {
        return j.getMarkers().findFirst(VarKeyword.class).isPresent();
    }

    private static boolean hasClause(J j, String name) {
        return j.getMarkers().getMarkers().stream().anyMatch(m -> m instanceof AttributeClause && ((AttributeClause) m).isNamed(name));
    }

    /**
     * The space before an element followed by the space its first part had.
     */
    static Space concat(Space outer, Space inner) {
        if (inner.isEmpty()) {
            return outer;
        }
        if (outer.getComments().isEmpty()) {
            return Space.build(outer.getWhitespace() + inner.getWhitespace(), inner.getComments());
        }
        List<Comment> comments = new ArrayList<>(outer.getComments());
        Comment last = comments.remove(comments.size() - 1);
        comments.add(last.withSuffix(last.getSuffix() + inner.getWhitespace()));
        comments.addAll(inner.getComments());
        return Space.build(outer.getWhitespace(), comments);
    }
}
