/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.TypeReferencePrefix;
import org.openrewrite.kotlin.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;

/**
 * Visit K types.
 */
public class KotlinVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof K.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "kotlin";
    }

    public J visitCompilationUnit(K.CompilationUnit cu, P p) {
        K.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withAnnotations(ListUtils.map(c.getAnnotations(), e -> visitAndCast(e, p)));
        if (c.getPadding().getPackageDeclaration() != null) {
            c = c.getPadding().withPackageDeclaration(visitRightPadded(c.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(ListUtils.map(c.getPadding().getStatements(), rp ->
                rp.withElement(Objects.requireNonNull(visitAndCast(rp.getElement(), p)))
                        .withAfter(visitSpace(rp.getAfter(), Space.Location.BLOCK_STATEMENT_SUFFIX, p))
        ));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("Kotlin has a different structure for its compilation unit. See K.CompilationUnit.");
    }

    public J visitAnnotatedExpression(K.AnnotatedExpression annotatedExpression, P p) {
        K.AnnotatedExpression ae = annotatedExpression;
        ae = ae.withMarkers(visitMarkers(ae.getMarkers(), p));
        ae = ae.withAnnotations(ListUtils.map(ae.getAnnotations(), a -> visitAndCast(a, p)));
        Expression temp = (Expression) visitExpression(ae, p);
        if (!(temp instanceof K.AnnotatedExpression)) {
            return temp;
        } else {
            ae = (K.AnnotatedExpression) temp;
        }
        return ae;
    }

    public J visitAnnotationType(K.AnnotationType annotationType, P p) {
        K.AnnotationType a = annotationType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withUseSite(visitRightPadded(a.getPadding().getUseSite(), JRightPadded.Location.ANNOTATION_ARGUMENT, p));
        return a.withCallee(visitAndCast(a.getCallee(), p));
    }

    public J visitBinary(K.Binary binary, P p) {
        K.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), KSpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof K.Binary)) {
            return temp;
        } else {
            b = (K.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), KLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitClassDeclaration(K.ClassDeclaration classDeclaration, P p) {
        K.ClassDeclaration c = classDeclaration;
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withClassDeclaration(visitAndCast(c.getClassDeclaration(), p));
        return c.withTypeConstraints(visitAndCast(c.getTypeConstraints(), p));
    }

    public J visitConstructor(K.Constructor constructor, P p) {
        K.Constructor c = constructor;
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withMethodDeclaration(visitAndCast(c.getMethodDeclaration(), p));
        return c.getPadding().withInvocation(visitLeftPadded(c.getPadding().getInvocation(), p));
    }

    public J visitConstructorInvocation(K.ConstructorInvocation constructorInvocation, P p) {
        K.ConstructorInvocation d = constructorInvocation;
        d = d.withPrefix(visitSpace(d.getPrefix(), KSpace.Location.CONSTRUCTOR_INVOCATION_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withTypeTree(visitAndCast(d.getTypeTree(), p));
        return d.getPadding().withArguments(visitContainer(d.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
    }

    public J visitDelegatedSuperType(K.DelegatedSuperType delegatedSuperType, P p) {
        K.DelegatedSuperType d = delegatedSuperType;
        d = d.withBy(visitSpace(d.getBy(), KSpace.Location.DELEGATED_SUPER_TYPE_BY, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withTypeTree(visitAndCast(d.getTypeTree(), p));
        return d.withDelegate(visitAndCast(d.getDelegate(), p));
    }

    public J visitDestructuringDeclaration(K.DestructuringDeclaration destructuringDeclaration, P p) {
        K.DestructuringDeclaration d = destructuringDeclaration;
        d = d.withPrefix(visitSpace(d.getPrefix(), KSpace.Location.DESTRUCTURING_DECLARATION_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        Statement temp = (Statement) visitStatement(d, p);
        if (!(temp instanceof K.DestructuringDeclaration)) {
            return temp;
        } else {
            d = (K.DestructuringDeclaration) temp;
        }
        d = d.withInitializer(visitAndCast(d.getInitializer(), p));
        return d.getPadding().withDestructAssignments(visitContainer(d.getPadding().getDestructAssignments(), KContainer.Location.DESTRUCT_ASSIGNMENTS, p));
    }

    public J visitFunctionType(K.FunctionType functionType, P p) {
        K.FunctionType f = functionType;
        f = f.withPrefix(visitSpace(f.getPrefix(), KSpace.Location.FUNCTION_TYPE_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withLeadingAnnotations(ListUtils.map(f.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        f = f.withModifiers(ListUtils.map(f.getModifiers(), e -> visitAndCast(e, p)));
        f = f.withReceiver(visitRightPadded(f.getReceiver(), p));
        if (f.getPadding().getParameters() != null) {
            f = f.getPadding().withParameters(visitContainer(f.getPadding().getParameters(), KContainer.Location.FUNCTION_TYPE_PARAMETERS, p));
        }
        return f.withReturnType(visitRightPadded(f.getReturnType(), p));
    }

    public J visitFunctionTypeParameter(K.FunctionType.Parameter parameter, P p) {
        K.FunctionType.Parameter pa = parameter;
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        if (pa.getName() != null) {
            pa = pa.withName(visitAndCast(pa.getName(), p));
        }
        return pa.withParameterType(visitAndCast(pa.getParameterType(), p));
    }

    public J visitListLiteral(K.ListLiteral listLiteral, P p) {
        K.ListLiteral l = listLiteral;
        l = l.withPrefix(visitSpace(l.getPrefix(), KSpace.Location.LIST_LITERAL_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof K.ListLiteral)) {
            return temp;
        } else {
            l = (K.ListLiteral) temp;
        }
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), KContainer.Location.LIST_LITERAL_ELEMENTS, p));
        return l.withType(visitType(l.getType(), p));
    }

    public J visitMethodDeclaration(K.MethodDeclaration methodDeclaration, P p) {
        K.MethodDeclaration m = methodDeclaration;
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.withMethodDeclaration(visitAndCast(m.getMethodDeclaration(), p));
        return m.withTypeConstraints(visitAndCast(m.getTypeConstraints(), p));
    }

    public J visitMultiAnnotationType(K.MultiAnnotationType multiAnnotationType, P p) {
        K.MultiAnnotationType m = multiAnnotationType;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withUseSite(visitRightPadded(m.getPadding().getUseSite(), JRightPadded.Location.ANNOTATION_ARGUMENT, p));
        return m.withAnnotations(visitContainer(m.getAnnotations(), p));
    }

    @SuppressWarnings("DataFlowIssue")
    public J visitProperty(K.Property property, P p) {
        K.Property pr = property;
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), KSpace.Location.PROPERTY_PREFIX, p));
        pr = pr.withMarkers(visitMarkers(pr.getMarkers(), p));
        Statement temp = (Statement) visitStatement(pr, p);
        if (!(temp instanceof K.Property)) {
            return temp;
        } else {
            pr = (K.Property) temp;
        }

        pr = pr.getPadding().withVariableDeclarations(visitRightPadded(pr.getPadding().getVariableDeclarations(), p));
        pr = pr.getPadding().withReceiver(visitRightPadded(pr.getPadding().getReceiver(), p));
        return pr.withAccessors(visitContainer(pr.getAccessors(), p));
    }

    public J visitReturn(K.Return return_, P p) {
        K.Return r = return_;
        r = r.withPrefix(visitSpace(r.getPrefix(), KSpace.Location.RETURN_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof K.Return)) {
            return temp;
        } else {
            r = (K.Return) temp;
        }
        Expression temp2 = (Expression) visitExpression(r, p);
        if (!(temp2 instanceof K.Return)) {
            return temp2;
        } else {
            r = (K.Return) temp2;
        }
        r = r.withExpression(visitAndCast(r.getExpression(), p));
        return r.withLabel(visitAndCast(r.getLabel(), p));
    }

    public J visitSpreadArgument(K.SpreadArgument spreadArgument, P p) {
        K.SpreadArgument s = spreadArgument;
        s = s.withPrefix(visitSpace(s.getPrefix(), KSpace.Location.SPREAD_ARGUMENT_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof K.SpreadArgument)) {
            return temp;
        } else {
            s = (K.SpreadArgument) temp;
        }
        return s.withExpression(visitAndCast(s.getExpression(), p));
    }

    public J visitStringTemplate(K.StringTemplate stringTemplate, P p) {
        K.StringTemplate k = stringTemplate;
        k = k.withPrefix(visitSpace(k.getPrefix(), KSpace.Location.STRING_TEMPLATE_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof K.StringTemplate)) {
            return temp;
        } else {
            k = (K.StringTemplate) temp;
        }
        k = k.withStrings(ListUtils.map(k.getStrings(), s -> visit(s, p)));
        return k.withType(visitType(k.getType(), p));
    }

    public J visitStringTemplateExpression(K.StringTemplate.Expression expression, P p) {
        K.StringTemplate.Expression v = expression;
        v = v.withPrefix(visitSpace(v.getPrefix(), KSpace.Location.STRING_TEMPLATE_EXPRESSION_PREFIX, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        return v.withAfter(visitSpace(v.getAfter(), KSpace.Location.STRING_TEMPLATE_EXPRESSION_AFTER, p));
    }

    public J visitThis(K.This aThis, P p) {
        K.This k = aThis;
        k = k.withPrefix(visitSpace(k.getPrefix(), KSpace.Location.THIS_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof K.This)) {
            return temp;
        } else {
            k = (K.This) temp;
        }
        return k.withType(visitType(k.getType(), p));
    }

    public J visitTypeAlias(K.TypeAlias typeAlias, P p) {
        K.TypeAlias t = typeAlias;
        t = t.withPrefix(visitSpace(t.getPrefix(), KSpace.Location.TYPE_ALIAS_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof K.TypeAlias)) {
            return temp;
        } else {
            t = (K.TypeAlias) temp;
        }
        t = t.withLeadingAnnotations(ListUtils.map(t.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        t = t.withModifiers(ListUtils.map(t.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        t = t.withModifiers(ListUtils.map(t.getModifiers(), m -> visitAndCast(m, p)));
        t = t.withName(visitAndCast(t.getName(), p));
        if (t.getPadding().getTypeParameters() != null) {
            t = t.getPadding().withTypeParameters(visitContainer(t.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        if (t.getPadding().getInitializer() != null) {
            t = t.getPadding().withInitializer(visitLeftPadded(t.getPadding().getInitializer(), KLeftPadded.Location.TYPE_ALIAS_INITIALIZER, p));
        }
        return t.withType(visitType(t.getType(), p));
    }

    public J visitTypeConstraints(K.TypeConstraints typeConstraints, P p) {
        K.TypeConstraints t = typeConstraints;
        t = t.withPrefix(visitSpace(t.getPrefix(), KSpace.Location.TYPE_CONSTRAINT_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return t.getPadding().withConstraints(visitContainer(t.getPadding().getConstraints(), p));
    }

    public J visitUnary(K.Unary unary, P p) {
        K.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), KSpace.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Statement temp = (Statement) visitStatement(u, p);
        if (!(temp instanceof K.Unary)) {
            return temp;
        } else {
            u = (K.Unary) temp;
        }
        Expression temp2 = (Expression) visitExpression(u, p);
        if (!(temp2 instanceof K.Unary)) {
            return temp2;
        } else {
            u = (K.Unary) temp2;
        }
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p));
        u = u.withExpression(visitAndCast(u.getExpression(), p));
        return u.withType(visitType(u.getType(), p));
    }

    public J visitWhen(K.When when, P p) {
        K.When w = when;
        w = w.withPrefix(visitSpace(w.getPrefix(), KSpace.Location.WHEN_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Statement temp = (Statement) visitStatement(w, p);
        if (!(temp instanceof K.When)) {
            return temp;
        } else {
            w = (K.When) temp;
        }
        w = w.withSelector(visitAndCast(w.getSelector(), p));
        w = w.withBranches(visitAndCast(w.getBranches(), p));
        return w.withType(visitType(w.getType(), p));
    }

    public J visitWhenBranch(K.WhenBranch whenBranch, P p) {
        K.WhenBranch w = whenBranch;
        w = w.withPrefix(visitSpace(w.getPrefix(), KSpace.Location.WHEN_BRANCH_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Statement temp = (Statement) visitStatement(w, p);
        if (!(temp instanceof K.WhenBranch)) {
            return temp;
        } else {
            w = (K.WhenBranch) temp;
        }
        w = w.getPadding().withExpressions(visitContainer(w.getPadding().getExpressions(), KContainer.Location.WHEN_BRANCH_EXPRESSION, p));
        return w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), JRightPadded.Location.CASE_BODY, p));
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public Space visitSpace(Space space, KSpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                        KContainer.Location loc, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, KLeftPadded.Location loc, P p) {
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElement();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            // If nothing changed leave AST node the same
            if (left.getElement() == null && before == left.getBefore()) {
                return left;
            }
            //noinspection ConstantConditions
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new JLeftPadded<>(before, t, left.getMarkers());
    }

    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, KRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (after == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ?
                right : new JRightPadded<>(t, after, markers);
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        Marker m = super.visitMarker(marker, p);
        if (marker instanceof TypeReferencePrefix) {
            TypeReferencePrefix tr = (TypeReferencePrefix) marker;
            m = tr.withPrefix(visitSpace(tr.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));
        }
        //noinspection unchecked
        return (M) m;
    }
}
