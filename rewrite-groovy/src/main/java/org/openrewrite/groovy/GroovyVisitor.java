/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

public class GroovyVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof G.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "groovy";
    }

    public J visitCompilationUnit(G.CompilationUnit cu, P p) {
        G.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPackageDeclaration() != null) {
            c = c.withPackageDeclaration((G.Package) visitNonNull(c.getPackageDeclaration(), p));
        }
        c = c.withStatements(ListUtils.map(c.getStatements(), e -> visitAndCast(e, p)));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("Groovy has a different structure for its compilation unit. See G.CompilationUnit.");
    }

    public J visitGString(G.GString gString, P p) {
        G.GString g = gString;
        g = g.withPrefix(visitSpace(g.getPrefix(), GSpace.Location.GSTRING, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        Expression temp = (Expression) visitExpression(g, p);
        if (!(temp instanceof G.GString)) {
            return temp;
        } else {
            g = (G.GString) temp;
        }
        g = g.withStrings(ListUtils.map(g.getStrings(), s -> visit(s, p)));
        return g.withType(visitType(gString.getType(), p));
    }

    public J visitGStringValue(G.GString.Value value, P p) {
        G.GString.Value v = value;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        return v.withAfter(visitSpace(v.getAfter(), GSpace.Location.GSTRING, p));
    }

    public J visitListLiteral(G.ListLiteral listLiteral, P p) {
        G.ListLiteral l = listLiteral;
        l = l.withPrefix(visitSpace(l.getPrefix(), GSpace.Location.LIST_LITERAL, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof G.ListLiteral)) {
            return temp;
        } else {
            l = (G.ListLiteral) temp;
        }
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), GContainer.Location.LIST_LITERAL_ELEMENTS, p));
        return l.withType(visitType(l.getType(), p));
    }

    public J visitMapEntry(G.MapEntry mapEntry, P p) {
        G.MapEntry m = mapEntry;
        m = m.withPrefix(visitSpace(m.getPrefix(), GSpace.Location.MAP_ENTRY, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Expression temp = (Expression) visitExpression(m, p);
        if (!(temp instanceof G.MapEntry)) {
            return temp;
        } else {
            m = (G.MapEntry) temp;
        }
        m = m.getPadding().withKey(visitRightPadded(m.getPadding().getKey(), GRightPadded.Location.MAP_ENTRY_KEY, p));
        m = m.withValue((Expression) visit(m.getValue(), p));
        return m.withType(visitType(m.getType(), p));
    }

    public J visitMapLiteral(G.MapLiteral mapLiteral, P p) {
        G.MapLiteral m = mapLiteral;
        m = m.withPrefix(visitSpace(m.getPrefix(), GSpace.Location.MAP_LITERAL, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Expression temp = (Expression) visitExpression(m, p);
        if (!(temp instanceof G.MapLiteral)) {
            return temp;
        } else {
            m = (G.MapLiteral) temp;
        }
        m = m.getPadding().withElements(visitContainer(m.getPadding().getElements(), GContainer.Location.MAP_LITERAL_ELEMENTS, p));
        return m.withType(visitType(m.getType(), p));
    }

    public J visitUnary(G.Unary unary, P p) {
        G.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), GSpace.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Expression temp = (Expression) visitExpression(u, p);
        if (!(temp instanceof G.Unary)) {
            return temp;
        } else {
            u = (G.Unary) temp;
        }
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), GLeftPadded.Location.UNARY_OPERATOR, p));
        u = u.withExpression(visitAndCast(u.getExpression(), p));
        return u.withType(visitType(u.getType(), p));
    }

    public J visitBinary(G.Binary binary, P p) {
        G.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), GSpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof G.Binary)) {
            return temp;
        } else {
            b = (G.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), GLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitTupleExpression(G.TupleExpression tuple, P p) {
        G.TupleExpression t = tuple;
        t = t.withPrefix(visitSpace(t.getPrefix(), GSpace.Location.TUPLE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof G.TupleExpression)) {
            return temp;
        } else {
            t = (G.TupleExpression) temp;
        }
        t = t.getPadding().withVariables(visitContainer(t.getPadding().getVariables(), GContainer.Location.TUPLE_ELEMENTS, p));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitRange(G.Range range, P p) {
        G.Range r = range;
        r = r.withPrefix(visitSpace(r.getPrefix(), GSpace.Location.RANGE_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Expression temp = (Expression) visitExpression(r, p);
        if (!(temp instanceof G.Range)) {
            return temp;
        } else {
            r = (G.Range) temp;
        }
        r = r.withFrom(visitAndCast(r.getFrom(), p));
        r = r.getPadding().withInclusive(visitLeftPadded(r.getPadding().getInclusive(), GLeftPadded.Location.RANGE_INCLUSION, p));
        r = r.withTo(visitAndCast(r.getTo(), p));
        return r.withType(visitType(r.getType(), p));
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, GRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, GLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public Space visitSpace(Space space, GSpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container,
                                                        GContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }
}
