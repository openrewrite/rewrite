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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.golang.tree.G;

public class GolangVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof G.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "go";
    }

    // ---------------------------------------------------------------
    // Go-specific visit methods
    // ---------------------------------------------------------------

    public J visitGoCompilationUnit(G.CompilationUnit cu, P p) {
        G.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getPackageDecl() != null) {
            c = c.getPadding().withPackageDecl(visitRightPadded(c.getPadding().getPackageDecl(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(),
                el -> visitRightPadded(el, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(visitStatements(c.getPadding().getStatements(), p));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    private <T extends Statement> java.util.List<JRightPadded<T>> visitStatements(
            java.util.List<JRightPadded<T>> statements, P p) {
        java.util.List<JRightPadded<T>> s = statements;
        for (int i = 0; i < s.size(); i++) {
            JRightPadded<T> rp = s.get(i);
            @SuppressWarnings("unchecked")
            T elem = (T) visitAndCast(rp.getElement(), p);
            if (elem != rp.getElement()) {
                if (s == statements) {
                    s = new java.util.ArrayList<>(statements);
                }
                s.set(i, rp.withElement(elem));
            }
        }
        return s;
    }

    public J visitGoStatement(G.GoStatement goStmt, P p) {
        G.GoStatement g = goStmt;
        g = g.withPrefix(visitSpace(g.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        g = g.withExpression((Expression) visitAndCast(g.getExpression(), p));
        return g;
    }

    public J visitDefer(G.Defer defer, P p) {
        G.Defer d = defer;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withExpression((Expression) visitAndCast(d.getExpression(), p));
        return d;
    }

    public J visitSend(G.Send send, P p) {
        G.Send s = send;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withChannelExpr((Expression) visitAndCast(s.getChannelExpr(), p));
        s = s.getPadding().withArrow(visitLeftPadded(s.getPadding().getArrow(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return s;
    }

    public J visitGoto(G.Goto gotoStmt, P p) {
        G.Goto g = gotoStmt;
        g = g.withPrefix(visitSpace(g.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        g = g.withLabelIdent((J.Identifier) visitAndCast(g.getLabelIdent(), p));
        return g;
    }

    public J visitFallthrough(G.Fallthrough fallthrough, P p) {
        G.Fallthrough f = fallthrough;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        return f;
    }

    public J visitComposite(G.Composite composite, P p) {
        G.Composite c = composite;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getTypeExpr() != null) {
            c = c.withTypeExpr((Expression) visitAndCast(c.getTypeExpr(), p));
        }
        c = c.getPadding().withElements(visitContainer(c.getPadding().getElements(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return c;
    }

    public J visitKeyValue(G.KeyValue keyValue, P p) {
        G.KeyValue kv = keyValue;
        kv = kv.withPrefix(visitSpace(kv.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        kv = kv.withMarkers(visitMarkers(kv.getMarkers(), p));
        kv = kv.withKeyExpr((Expression) visitAndCast(kv.getKeyExpr(), p));
        kv = kv.getPadding().withValue(visitLeftPadded(kv.getPadding().getValue(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return kv;
    }

    public J visitSliceExpr(G.SliceExpr slice, P p) {
        G.SliceExpr s = slice;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIndexed((Expression) visitAndCast(s.getIndexed(), p));
        s = s.withOpenBracket(visitSpace(s.getOpenBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.getPadding().withLow(visitRightPadded(s.getPadding().getLow(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        s = s.getPadding().withHigh(visitRightPadded(s.getPadding().getHigh(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        if (s.getMax() != null) {
            s = s.withMax((Expression) visitAndCast(s.getMax(), p));
        }
        s = s.withCloseBracket(visitSpace(s.getCloseBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        return s;
    }

    public J visitMapType(G.MapType mapType, P p) {
        G.MapType m = mapType;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.withOpenBracket(visitSpace(m.getOpenBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.getPadding().withKey(visitRightPadded(m.getPadding().getKey(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        m = m.withValue((Expression) visitAndCast(m.getValue(), p));
        return m;
    }

    public J visitChannel(G.Channel channel, P p) {
        G.Channel c = channel;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withValue((Expression) visitAndCast(c.getValue(), p));
        return c;
    }

    public J visitFuncType(G.FuncType funcType, P p) {
        G.FuncType f = funcType;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.getPadding().withParameters(visitContainer(f.getPadding().getParameters(), JContainer.Location.LANGUAGE_EXTENSION, p));
        if (f.getReturnType() != null) {
            f = f.withReturnType((Expression) visitAndCast(f.getReturnType(), p));
        }
        return f;
    }

    public J visitStructType(G.StructType structType, P p) {
        G.StructType s = structType;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withBody((J.Block) visitAndCast(s.getBody(), p));
        return s;
    }

    public J visitInterfaceType(G.InterfaceType interfaceType, P p) {
        G.InterfaceType i = interfaceType;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withBody((J.Block) visitAndCast(i.getBody(), p));
        return i;
    }

    public J visitTypeList(G.TypeList typeList, P p) {
        G.TypeList t = typeList;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.getPadding().withTypes(visitContainer(t.getPadding().getTypes(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return t;
    }

    public J visitTypeDecl(G.TypeDecl typeDecl, P p) {
        G.TypeDecl t = typeDecl;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withName((J.Identifier) visitAndCast(t.getName(), p));
        if (t.getDefinition() != null) {
            t = t.withDefinition((Expression) visitAndCast(t.getDefinition(), p));
        }
        if (t.getPadding().getSpecs() != null) {
            t = t.getPadding().withSpecs(visitContainer(t.getPadding().getSpecs(), JContainer.Location.LANGUAGE_EXTENSION, p));
        }
        return t;
    }

    public J visitMultiAssignment(G.MultiAssignment multiAssignment, P p) {
        G.MultiAssignment m = multiAssignment;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        return m;
    }

    public J visitCommClause(G.CommClause commClause, P p) {
        G.CommClause c = commClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getComm() != null) {
            c = c.withComm((Statement) visitAndCast(c.getComm(), p));
        }
        c = c.withColon(visitSpace(c.getColon(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.getPadding().withBody(visitStatements(c.getPadding().getBody(), p));
        return c;
    }

    public J visitIndexList(G.IndexList indexList, P p) {
        G.IndexList i = indexList;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withTarget((Expression) visitAndCast(i.getTarget(), p));
        i = i.getPadding().withIndices(visitContainer(i.getPadding().getIndices(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return i;
    }
}
