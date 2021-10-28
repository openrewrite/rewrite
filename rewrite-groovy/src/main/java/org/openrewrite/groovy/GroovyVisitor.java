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

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.groovy.tree.GContainer;
import org.openrewrite.groovy.tree.GRightPadded;
import org.openrewrite.groovy.tree.GSpace;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

public class GroovyVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof G.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "groovy";
    }

    @Override
    public J visitJavaSourceFile(JavaSourceFile cu, P p) {
        return cu instanceof G.CompilationUnit ? visitCompilationUnit((G.CompilationUnit) cu, p) : cu;
    }

    public J visitCompilationUnit(G.CompilationUnit cu, P p) {
        G.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withStatements(ListUtils.map(c.getStatements(), e -> visitAndCast(e, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    @Override
    public final J visitCompilationUnit(J.CompilationUnit cu, P p) {
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
        visit(g.getStrings(), p);
        return g;
    }

    public J visitGStringValue(G.GString.Value value, P p) {
        G.GString.Value v = value;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        return v;
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
        return l;
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
        return m;
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
        return m;
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, GRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public Space visitSpace(Space space, GSpace.Location loc, P p) {
        return super.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container,
                                                        GContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }
}
