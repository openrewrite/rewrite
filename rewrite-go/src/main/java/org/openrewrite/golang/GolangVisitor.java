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
import org.openrewrite.golang.marker.TrailingComma;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;

public class GolangVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Go.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "go";
    }

    // A TrailingComma stashes the whitespace around a trailing `,` (before the
    // closing `)`/`}`/`]`) in its own Space fields, which the Go printer emits.
    // Visit them so LST-walking code sees every space the printer does.
    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        if (marker instanceof TrailingComma) {
            TrailingComma tc = (TrailingComma) marker;
            //noinspection unchecked
            return (M) tc.withBefore(visitSpace(tc.getBefore(), Space.Location.LANGUAGE_EXTENSION, p))
                    .withAfter(visitSpace(tc.getAfter(), Space.Location.LANGUAGE_EXTENSION, p));
        }
        return super.visitMarker(marker, p);
    }

    // JavaVisitor.visitContainer visits the container's `before` and elements but
    // not the container's own markers, where a param-list TrailingComma lives.
    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                                  JContainer.Location loc, P p) {
        JContainer<J2> c = super.visitContainer(container, loc, p);
        if (c != null) {
            c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        }
        return c;
    }

    // ---------------------------------------------------------------
    // Go-specific visit methods
    // ---------------------------------------------------------------

    public J visitGoCompilationUnit(Go.CompilationUnit cu, P p) {
        Go.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getPackageDecl() != null) {
            c = c.getPadding().withPackageDecl(
                    visitRightPadded(c.getPadding().getPackageDecl(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        }
        if (c.getImportsContainer() != null) {
            c = c.withImportsContainer(c.getImportsContainer().withBefore(
                    visitSpace(c.getImportsContainer().getBefore(), Space.Location.LANGUAGE_EXTENSION, p)));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(),
                el -> visitRightPadded(el, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(visitStatements(c.getPadding().getStatements(), p));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    private <T extends Statement> java.util.List<JRightPadded<T>> visitStatements(
            java.util.List<JRightPadded<T>> statements, P p) {
        return ListUtils.map(statements, rp -> visitRightPadded(rp, JRightPadded.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitGoStatement(Go.GoStatement goStmt, P p) {
        Go.GoStatement g = goStmt;
        g = g.withPrefix(visitSpace(g.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        return g.withExpression((Expression) visitAndCast(g.getExpression(), p));
    }

    public J visitDefer(Go.Defer defer, P p) {
        Go.Defer d = defer;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d.withExpression((Expression) visitAndCast(d.getExpression(), p));
    }

    public J visitSend(Go.Send send, P p) {
        Go.Send s = send;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withChannelExpr((Expression) visitAndCast(s.getChannelExpr(), p));
        return s.getPadding().withArrow(visitLeftPadded(s.getPadding().getArrow(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitGoto(Go.Goto gotoStmt, P p) {
        Go.Goto g = gotoStmt;
        g = g.withPrefix(visitSpace(g.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        return g.withLabelIdent((J.Identifier) visitAndCast(g.getLabelIdent(), p));
    }

    public J visitFallthrough(Go.Fallthrough fallthrough, P p) {
        Go.Fallthrough f = fallthrough;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        return f.withMarkers(visitMarkers(f.getMarkers(), p));
    }

    public J visitComposite(Go.Composite composite, P p) {
        Go.Composite c = composite;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getTypeExpr() != null) {
            c = c.withTypeExpr((Expression) visitAndCast(c.getTypeExpr(), p));
        }
        return c.getPadding().withElements(visitContainer(c.getPadding().getElements(), JContainer.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitKeyValue(Go.KeyValue keyValue, P p) {
        Go.KeyValue kv = keyValue;
        kv = kv.withPrefix(visitSpace(kv.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        kv = kv.withMarkers(visitMarkers(kv.getMarkers(), p));
        kv = kv.withKeyExpr((Expression) visitAndCast(kv.getKeyExpr(), p));
        return kv.getPadding().withValue(visitLeftPadded(kv.getPadding().getValue(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitSliceExpr(Go.SliceExpr slice, P p) {
        Go.SliceExpr s = slice;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIndexed((Expression) visitAndCast(s.getIndexed(), p));
        s = s.withOpenBracket(visitSpace(s.getOpenBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.getPadding().withLow(visitRightPadded(s.getPadding().getLow(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        s = s.getPadding().withHigh(visitRightPadded(s.getPadding().getHigh(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        if (s.getMax() != null) {
            s = s.withMax((Expression) visitAndCast(s.getMax(), p));
        }
        return s.withCloseBracket(visitSpace(s.getCloseBracket(), Space.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitGoArrayType(Go.ArrayType arrayType, P p) {
        Go.ArrayType a = arrayType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withLength(visitRightPadded(a.getPadding().getLength(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        return a.withElementType((Expression) visitAndCast(a.getElementType(), p));
    }

    public J visitMapType(Go.MapType mapType, P p) {
        Go.MapType m = mapType;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.withOpenBracket(visitSpace(m.getOpenBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.getPadding().withKey(visitRightPadded(m.getPadding().getKey(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        return m.withValue((Expression) visitAndCast(m.getValue(), p));
    }

    public J visitStatementExpression(Go.StatementExpression statementExpression, P p) {
        Go.StatementExpression se = statementExpression;
        se = se.withPrefix(visitSpace(se.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        Expression tempExpression = (Expression) visitExpression(se, p);
        if (!(tempExpression instanceof Go.StatementExpression)) {
            return tempExpression;
        }
        se = (Go.StatementExpression) tempExpression;
        se = se.withMarkers(visitMarkers(se.getMarkers(), p));
        return se.withStatement((Statement) visitAndCast(se.getStatement(), p));
    }

    public J visitPointerType(Go.PointerType pointerType, P p) {
        Go.PointerType pt = pointerType;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        pt = pt.withMarkers(visitMarkers(pt.getMarkers(), p));
        return pt.withElem((Expression) visitAndCast(pt.getElem(), p));
    }

    public J visitChannel(Go.Channel channel, P p) {
        Go.Channel c = channel;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c.withValue((Expression) visitAndCast(c.getValue(), p));
    }

    public J visitFuncType(Go.FuncType funcType, P p) {
        Go.FuncType f = funcType;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.getPadding().withParameters(visitContainer(f.getPadding().getParameters(), JContainer.Location.LANGUAGE_EXTENSION, p));
        if (f.getReturnType() != null) {
            f = f.withReturnType((Expression) visitAndCast(f.getReturnType(), p));
        }
        return f;
    }

    public J visitStructType(Go.StructType structType, P p) {
        Go.StructType s = structType;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withBody((J.Block) visitAndCast(s.getBody(), p));
    }

    public J visitInterfaceType(Go.InterfaceType interfaceType, P p) {
        Go.InterfaceType i = interfaceType;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i.withBody((J.Block) visitAndCast(i.getBody(), p));
    }

    public J visitTypeList(Go.TypeList typeList, P p) {
        Go.TypeList t = typeList;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return t.getPadding().withTypes(visitContainer(t.getPadding().getTypes(), JContainer.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitUnion(Go.Union union, P p) {
        Go.Union u = union;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        return u.getPadding().withTypes(ListUtils.map(u.getPadding().getTypes(),
                t -> visitRightPadded(t, JRightPadded.Location.LANGUAGE_EXTENSION, p)));
    }

    public J visitUnderlyingType(Go.UnderlyingType underlyingType, P p) {
        Go.UnderlyingType ut = underlyingType;
        ut = ut.withPrefix(visitSpace(ut.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        ut = ut.withMarkers(visitMarkers(ut.getMarkers(), p));
        return ut.withElement((Expression) visitAndCast(ut.getElement(), p));
    }

    public J visitTypeDecl(Go.TypeDecl typeDecl, P p) {
        Go.TypeDecl t = typeDecl;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withLeadingAnnotations(ListUtils.map(t.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        t = t.withName((J.Identifier) visitAndCast(t.getName(), p));
        if (t.getTypeParameters() != null) {
            t = t.withTypeParameters((J.TypeParameters) visitAndCast(t.getTypeParameters(), p));
        }
        if (t.getPadding().getAssign() != null) {
            t = t.getPadding().withAssign(visitLeftPadded(t.getPadding().getAssign(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        }
        if (t.getDefinition() != null) {
            t = t.withDefinition((Expression) visitAndCast(t.getDefinition(), p));
        }
        if (t.getPadding().getSpecs() != null) {
            t = t.getPadding().withSpecs(visitContainer(t.getPadding().getSpecs(), JContainer.Location.LANGUAGE_EXTENSION, p));
        }
        return t;
    }

    public J visitDeclarationBlock(Go.DeclarationBlock declarationBlock, P p) {
        Go.DeclarationBlock d = declarationBlock;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withLeadingAnnotations(ListUtils.map(d.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        return d.getPadding().withSpecs(visitContainer(d.getPadding().getSpecs(), JContainer.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitMultiAssignment(Go.MultiAssignment multiAssignment, P p) {
        Go.MultiAssignment m = multiAssignment;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withVariables(ListUtils.map(m.getPadding().getVariables(),
                el -> visitRightPadded(el, JRightPadded.Location.LANGUAGE_EXTENSION, p)));
        m = m.getPadding().withOperator(visitLeftPadded(m.getPadding().getOperator(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return m.getPadding().withValues(ListUtils.map(m.getPadding().getValues(),
                el -> visitRightPadded(el, JRightPadded.Location.LANGUAGE_EXTENSION, p)));
    }

    public J visitGoReturn(Go.Return aReturn, P p) {
        Go.Return r = aReturn;
        r = r.withPrefix(visitSpace(r.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        return r.getPadding().withExpressions(ListUtils.map(r.getPadding().getExpressions(),
                el -> visitRightPadded(el, JRightPadded.Location.LANGUAGE_EXTENSION, p)));
    }

    public J visitGoMethodDeclaration(Go.MethodDeclaration methodDeclaration, P p) {
        Go.MethodDeclaration m = methodDeclaration;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withReceiver(visitContainer(m.getPadding().getReceiver(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return m.withDeclaration((J.MethodDeclaration) visitAndCast(m.getDeclaration(), p));
    }

    public J visitStatementWithInit(Go.StatementWithInit statementWithInit, P p) {
        Go.StatementWithInit s = statementWithInit;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withInit(visitRightPadded(s.getPadding().getInit(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        return s.withStatement((Statement) visitAndCast(s.getStatement(), p));
    }

    public J visitCommClause(Go.CommClause commClause, P p) {
        Go.CommClause c = commClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getComm() != null) {
            c = c.withComm((Statement) visitAndCast(c.getComm(), p));
        }
        c = c.withColon(visitSpace(c.getColon(), Space.Location.LANGUAGE_EXTENSION, p));
        return c.getPadding().withBody(visitStatements(c.getPadding().getBody(), p));
    }

    public J visitIndexList(Go.IndexList indexList, P p) {
        Go.IndexList i = indexList;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withTarget((Expression) visitAndCast(i.getTarget(), p));
        return i.getPadding().withIndices(visitContainer(i.getPadding().getIndices(), JContainer.Location.LANGUAGE_EXTENSION, p));
    }

    public J visitGoUnary(Go.Unary unary, P p) {
        Go.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return u.withExpression((Expression) visitAndCast(u.getExpression(), p));
    }

    public J visitGoBinary(Go.Binary binary, P p) {
        Go.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.withLeft((Expression) visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return b.withRight((Expression) visitAndCast(b.getRight(), p));
    }

    public J visitGoAssignmentOperation(Go.AssignmentOperation assignOp, P p) {
        Go.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withVariable((Expression) visitAndCast(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return a.withAssignment((Expression) visitAndCast(a.getAssignment(), p));
    }

    public J visitGoVariadic(Go.Variadic variadic, P p) {
        Go.Variadic v = variadic;
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withElement((Expression) visitAndCast(v.getElement(), p));
        return v.withDots(visitSpace(v.getDots(), Space.Location.LANGUAGE_EXTENSION, p));
    }
}
