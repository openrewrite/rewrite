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

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.AnnotationCallSite;
import org.openrewrite.kotlin.tree.*;

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

    @Override
    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = annotation;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.Annotation)) {
            return temp;
        } else {
            a = (J.Annotation) temp;
        }

        a = a.withMarkers(a.getMarkers().withMarkers(ListUtils.map(a.getMarkers().getMarkers(), m -> {
            if (m instanceof AnnotationCallSite) {
                AnnotationCallSite acs = (AnnotationCallSite) m;
                acs = acs.withSuffix(visitSpace(acs.getSuffix(), Space.Location.LANGUAGE_EXTENSION, p));
                return acs;
            }
            return m;
        })));

        if (a.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(visitContainer(a.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, p));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), p));
        a = a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
        return a;
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
}
