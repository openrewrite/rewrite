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

import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.service.KotlinAutoFormatService;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Marker;

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
        c = c.withStatements(ListUtils.map(c.getStatements(), e -> visitAndCast(e, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("Kotlin has a different structure for its compilation unit. See K.CompilationUnit.");
    }

    @Override
    public <J2 extends J> J2 autoFormat(J2 j, P p) {
        return autoFormat(j, p, getCursor().getParentTreeCursor());
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public <J2 extends J> J2 autoFormat(J2 j, @Nullable J stopAfter, P p, Cursor cursor) {
        KotlinAutoFormatService service = getCursor().firstEnclosingOrThrow(JavaSourceFile.class).service(KotlinAutoFormatService.class);
        return (J2) service.autoFormatVisitor(stopAfter).visit(j, p, cursor);
    }

    @Override
    public <J2 extends J> J2 autoFormat(J2 j, P p, Cursor cursor) {
        return autoFormat(j, null, p, cursor);
    }

    @Override
    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, P p) {
        return maybeAutoFormat(before, after, p, getCursor().getParentTreeCursor());
    }

    @Override
    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, P p, Cursor cursor) {
        return maybeAutoFormat(before, after, null, p, cursor);
    }

    @Override
    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, @Nullable J stopAfter, P p, Cursor cursor) {
        if (before != after) {
            return autoFormat(after, stopAfter, p, cursor);
        }
        return after;
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
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
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
        d = d.getPadding().withAssignments(visitContainer(d.getPadding().getAssignments(), p));
        return d;
    }

    public J visitFunctionType(K.FunctionType functionType, P p) {
        K.FunctionType f = functionType;
        f = f.withPrefix(visitSpace(f.getPrefix(), KSpace.Location.FUNCTION_TYPE_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof K.FunctionType)) {
            return temp;
        } else {
            f = (K.FunctionType) temp;
        }
        f = f.withLeadingAnnotations(ListUtils.map(f.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        f = f.withModifiers(ListUtils.map(f.getModifiers(), e -> visitAndCast(e, p)));
        f = f.withReceiver(visitRightPadded(f.getReceiver(), p));
        f = f.withTypedTree(visitAndCast(f.getTypedTree(), p));
        return f;
    }

    public J visitKReturn(K.KReturn kReturn, P p) {
        K.KReturn r = kReturn;
        r = r.withPrefix(visitSpace(r.getPrefix(), KSpace.Location.KRETURN_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof K.KReturn)) {
            return temp;
        } else {
            r = (K.KReturn) temp;
        }
        r = r.withAnnotations(ListUtils.map(r.getAnnotations(), a -> visitAndCast(a, p)));
        r = r.withExpression(visitAndCast(r.getExpression(), p));
        r = r.withLabel(visitAndCast(r.getLabel(), p));
        return r;
    }

    public J visitKString(K.KString kString, P p) {
        K.KString k = kString;
        k = k.withPrefix(visitSpace(k.getPrefix(), KSpace.Location.KSTRING_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof K.KString)) {
            return temp;
        } else {
            k = (K.KString) temp;
        }
        k = k.withStrings(ListUtils.map(k.getStrings(), s -> visit(s, p)));
        k = k.withType(visitType(k.getType(), p));
        return k;
    }

    public J visitKThis(K.KThis kThis, P p) {
        K.KThis k = kThis;
        k = k.withPrefix(visitSpace(k.getPrefix(), KSpace.Location.KTHIS_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof K.KThis)) {
            return temp;
        } else {
            k = (K.KThis) temp;
        }
        k = k.withType(visitType(k.getType(), p));
        return k;
    }

    public J visitKStringValue(K.KString.Value value, P p) {
        K.KString.Value v = value;
        v = v.withPrefix(visitSpace(v.getPrefix(), KSpace.Location.KSTRING_VALUE_PREFIX, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        v = v.withAfter(visitSpace(v.getAfter(), KSpace.Location.KSTRING_VALUE_AFTER, p));
        return v;
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
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), p));
        l = l.withType(visitType(l.getType(), p));
        return l;
    }

    public J visitNamedVariableInitializer(K.NamedVariableInitializer namedVariableInitializer, P p) {
        K.NamedVariableInitializer n = namedVariableInitializer;
        n = n.withPrefix(visitSpace(n.getPrefix(), KSpace.Location.NAMED_VARIABLE_INITIALIZER_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        n = n.withInitializations(ListUtils.map(n.getInitializations(), it -> visitAndCast(it, p)));
        return n;
    }

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
        pr = pr.withVariableDeclarations(visitAndCast(pr.getVariableDeclarations(), p));
        pr = pr.withGetter(visitAndCast(pr.getGetter(), p));
        pr = pr.withSetter(visitAndCast(pr.getSetter(), p));
        return pr;
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
        w = w.withType(visitType(w.getType(), p));
        return w;
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
        w = w.getPadding().withExpressions(visitContainer(w.getPadding().getExpressions(), p));
        w = w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), JRightPadded.Location.CASE_BODY, p));
        return w;
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

    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        Marker m = super.visitMarker(marker, p);
        if (m instanceof AnnotationCallSite) {
            AnnotationCallSite acs = (AnnotationCallSite) marker;
            m = acs.withSuffix(visitSpace(acs.getSuffix(), KSpace.Location.ANNOTATION_CALL_SITE_PREFIX, p));
        } else if (marker instanceof CheckNotNull) {
            CheckNotNull cnn = (CheckNotNull) marker;
            m = cnn.withPrefix(visitSpace(cnn.getPrefix(), KSpace.Location.CHECK_NOT_NULL_PREFIX, p));
        } else if (marker instanceof ConstructorDelegation) {
            ConstructorDelegation cd = (ConstructorDelegation) marker;
            m = cd.withPrefix(visitSpace(cd.getPrefix(), KSpace.Location.CONSTRUCTOR_DELEGATION_PREFIX, p));
        } else if (marker instanceof IsNullable) {
            IsNullable isn = (IsNullable) marker;
            m = isn.withPrefix(visitSpace(isn.getPrefix(), KSpace.Location.IS_NULLABLE_PREFIX, p));
        } else if (marker instanceof IsNullSafe) {
            IsNullSafe ins = (IsNullSafe) marker;
            m = ins.withPrefix(visitSpace(ins.getPrefix(), KSpace.Location.IS_NULLABLE_PREFIX, p));
        } else if (marker instanceof KObject) {
            KObject ko = (KObject) marker;
            m = ko.withPrefix(visitSpace(ko.getPrefix(), KSpace.Location.OBJECT_PREFIX, p));
        } else if (marker instanceof SpreadArgument) {
            SpreadArgument sa = (SpreadArgument) marker;
            m = sa.withPrefix(visitSpace(sa.getPrefix(), KSpace.Location.SPREAD_ARGUMENT_PREFIX, p));
        } else if (marker instanceof TypeReferencePrefix) {
            TypeReferencePrefix tr = (TypeReferencePrefix) marker;
            m = tr.withPrefix(visitSpace(tr.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));
        }
        //noinspection unchecked
        return (M) m;
    }
}
