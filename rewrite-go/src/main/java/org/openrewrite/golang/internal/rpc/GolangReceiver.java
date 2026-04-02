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
package org.openrewrite.golang.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

public class GolangReceiver extends GolangVisitor<RpcReceiveQueue> {
    private final GolangReceiverDelegate delegate = new GolangReceiverDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
        if (tree instanceof Go) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        if (j instanceof Go.StatementExpression) {
            return j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        }
        return ((J) j.withId(q.receiveAndGet(j.getId(), UUID::fromString)))
                .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public J visitGoCompilationUnit(Go.CompilationUnit cu, RpcReceiveQueue q) {
        return cu.withSourcePath(q.<Path, String>receiveAndGet(cu.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(cu.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<Go.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .getPadding().withPackageDecl(q.receive(cu.getPadding().getPackageDecl(), el -> visitRightPadded(el, q)))
                .withImportsContainer(q.receive(cu.getImportsContainer(), c -> visitContainer(c, q)))
                .getPadding().withStatements(q.receiveList(cu.getPadding().getStatements(), stmt -> visitRightPadded(stmt, q)))
                .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitGoStatement(Go.GoStatement goStmt, RpcReceiveQueue q) {
        return goStmt
                .withExpression(q.receive(goStmt.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitDefer(Go.Defer defer, RpcReceiveQueue q) {
        return defer
                .withExpression(q.receive(defer.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitSend(Go.Send send, RpcReceiveQueue q) {
        return send
                .withChannelExpr(q.receive(send.getChannelExpr(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withArrow(q.receive(send.getPadding().getArrow(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitGoto(Go.Goto gotoStmt, RpcReceiveQueue q) {
        return gotoStmt
                .withLabelIdent(q.receive(gotoStmt.getLabelIdent(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitFallthrough(Go.Fallthrough fallthrough, RpcReceiveQueue q) {
        return fallthrough;
    }

    @Override
    public J visitComposite(Go.Composite composite, RpcReceiveQueue q) {
        return composite
                .withTypeExpr(q.receive(composite.getTypeExpr(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withElements(q.receive(composite.getPadding().getElements(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitKeyValue(Go.KeyValue keyValue, RpcReceiveQueue q) {
        return keyValue
                .withKeyExpr(q.receive(keyValue.getKeyExpr(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withValue(q.receive(keyValue.getPadding().getValue(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitSliceExpr(Go.SliceExpr slice, RpcReceiveQueue q) {
        return slice
                .withIndexed(q.receive(slice.getIndexed(), expr -> (Expression) visitNonNull(expr, q)))
                .withOpenBracket(q.receive(slice.getOpenBracket(), space -> visitSpace(space, q)))
                .getPadding().withLow(q.receive(slice.getPadding().getLow(), el -> visitRightPadded(el, q)))
                .getPadding().withHigh(q.receive(slice.getPadding().getHigh(), el -> visitRightPadded(el, q)))
                .withMax(q.receive(slice.getMax(), expr -> (Expression) visitNonNull(expr, q)))
                .withCloseBracket(q.receive(slice.getCloseBracket(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitMapType(Go.MapType mapType, RpcReceiveQueue q) {
        return mapType
                .withOpenBracket(q.receive(mapType.getOpenBracket(), space -> visitSpace(space, q)))
                .getPadding().withKey(q.receive(mapType.getPadding().getKey(), el -> visitRightPadded(el, q)))
                .withValue(q.receive(mapType.getValue(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitPointerType(Go.PointerType pointerType, RpcReceiveQueue q) {
        return pointerType
                .withElem(q.receive(pointerType.getElem(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitChannel(Go.Channel channel, RpcReceiveQueue q) {
        return channel
                .withDir(q.receiveAndGet(channel.getDir(), v -> Go.ChanDir.valueOf((String) v)))
                .withValue(q.receive(channel.getValue(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitFuncType(Go.FuncType funcType, RpcReceiveQueue q) {
        return funcType
                .getPadding().withParameters(q.receive(funcType.getPadding().getParameters(), el -> visitContainer(el, q)))
                .withReturnType(q.receive(funcType.getReturnType(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitStructType(Go.StructType structType, RpcReceiveQueue q) {
        return structType
                .withBody(q.receive(structType.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitInterfaceType(Go.InterfaceType interfaceType, RpcReceiveQueue q) {
        return interfaceType
                .withBody(q.receive(interfaceType.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitTypeList(Go.TypeList typeList, RpcReceiveQueue q) {
        return typeList
                .getPadding().withTypes(q.receive(typeList.getPadding().getTypes(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitTypeDecl(Go.TypeDecl typeDecl, RpcReceiveQueue q) {
        return typeDecl
                .withName(q.receive(typeDecl.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withAssign(q.receive(typeDecl.getPadding().getAssign(), el -> visitLeftPadded(el, q)))
                .withDefinition(q.receive(typeDecl.getDefinition(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withSpecs(q.receive(typeDecl.getPadding().getSpecs(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitMultiAssignment(Go.MultiAssignment multiAssignment, RpcReceiveQueue q) {
        return multiAssignment
                .getPadding().withVariables(q.receiveList(multiAssignment.getPadding().getVariables(), v -> visitRightPadded(v, q)))
                .getPadding().withOperator(q.receive(multiAssignment.getPadding().getOperator(), el -> visitLeftPadded(el, q)))
                .getPadding().withValues(q.receiveList(multiAssignment.getPadding().getValues(), v -> visitRightPadded(v, q)));
    }

    @Override
    public J visitCommClause(Go.CommClause commClause, RpcReceiveQueue q) {
        return commClause
                .withComm(q.receive(commClause.getComm(), el -> (Statement) visitNonNull(el, q)))
                .withColon(q.receive(commClause.getColon(), space -> visitSpace(space, q)))
                .getPadding().withBody(q.receiveList(commClause.getPadding().getBody(), stmt -> visitRightPadded(stmt, q)));
    }

    @Override
    public J visitStatementExpression(Go.StatementExpression stmtExpr, RpcReceiveQueue q) {
        return stmtExpr
                .withStatement(q.receive(stmtExpr.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)));
    }

    @Override
    public J visitIndexList(Go.IndexList indexList, RpcReceiveQueue q) {
        return indexList
                .withTarget(q.receive(indexList.getTarget(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withIndices(q.receive(indexList.getPadding().getIndices(), el -> visitContainer(el, q)));
    }

    // Delegation methods to JavaReceiver for RPC-specific visit methods
    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q) {
        return delegate.visitLeftPadded(left, q);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q, java.util.function.Function<Object, T> mapValue) {
        return delegate.visitLeftPadded(left, q, mapValue);
    }

    public <T> JRightPadded<T> visitRightPadded(JRightPadded<T> right, RpcReceiveQueue q) {
        return delegate.visitRightPadded(right, q);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, RpcReceiveQueue q) {
        return delegate.visitContainer(container, q);
    }

    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return delegate.visitSpace(space, q);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcReceiveQueue q) {
        return delegate.visitType(javaType, q);
    }

    private static class GolangReceiverDelegate extends JavaReceiver {
        private final GolangReceiver delegate;

        public GolangReceiverDelegate(GolangReceiver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
            if (tree instanceof Go) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public J visitForEachControl(J.ForEachLoop.Control control, RpcReceiveQueue q) {
            // Go sends: key (right-padded), value (right-padded), operator (left-padded string), iterable
            // Read these and construct a valid ForEachLoop.Control

            // key (right-padded Expression, nullable)
            @SuppressWarnings({"unchecked", "rawtypes"})
            JRightPadded<Expression> key = (JRightPadded<Expression>) ((RpcReceiveQueue) q).receive(
                    null, (java.util.function.UnaryOperator) el -> visitRightPadded((JRightPadded<?>) el, q));
            // value (right-padded Expression, nullable)
            @SuppressWarnings({"unchecked", "rawtypes"})
            JRightPadded<Expression> value = (JRightPadded<Expression>) ((RpcReceiveQueue) q).receive(
                    null, (java.util.function.UnaryOperator) el -> visitRightPadded((JRightPadded<?>) el, q));
            // operator (left-padded string - AssignOp, skip it)
            q.receive(null);
            // iterable (Expression)
            Expression iterable = q.receive(null, el -> (Expression) visitNonNull(el, q));
            if (iterable == null) {
                iterable = new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY);
            }

            // Build a synthetic VariableDeclarations to represent key/value
            J.Identifier varName;
            if (key != null && key.getElement() instanceof J.Identifier) {
                varName = (J.Identifier) key.getElement();
            } else {
                varName = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        Collections.emptyList(), "_", null, null);
            }

            J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                    Tree.randomId(), varName.getPrefix(), varName.getMarkers(),
                    varName.withPrefix(Space.EMPTY), Collections.emptyList(), null, null);

            J.VariableDeclarations varDecls = new J.VariableDeclarations(
                    Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                    Collections.emptyList(), Collections.emptyList(), null,
                    null, Collections.emptyList(),
                    Collections.singletonList(JRightPadded.build(namedVar)));

            Space afterVar = key != null ? key.getAfter() : Space.EMPTY;

            @SuppressWarnings("unchecked")
            JRightPadded<Statement> varPadded = (JRightPadded<Statement>) (JRightPadded<?>) JRightPadded.build(varDecls).withAfter(afterVar);
            return control
                    .getPadding().withVariable(varPadded)
                    .getPadding().withIterable(JRightPadded.build(iterable));
        }

        @Override
        public J visitImport(J.Import importStmt, RpcReceiveQueue q) {
            importStmt = importStmt.getPadding().withStatic(
                    q.receive(importStmt.getPadding().getStatic(), s -> visitLeftPadded(s, q)));

            // Go sends qualid as a Literal (import path string). Convert to FieldAccess for Java model.
            // Use raw types to bypass UnaryOperator bridge method ClassCastException
            @SuppressWarnings({"unchecked", "rawtypes"})
            J.FieldAccess qualid = (J.FieldAccess) ((RpcReceiveQueue) q).receive(
                    (Object) importStmt.getQualid(),
                    (java.util.function.UnaryOperator) id -> {
                        J received = visitNonNull((J) id, q);
                        if (received instanceof J.FieldAccess) {
                            return received;
                        }
                        // Convert Literal to FieldAccess for Java model
                        J.Literal lit = (J.Literal) received;
                        String name = lit.getValueSource() != null ?
                                lit.getValueSource().replaceAll("^\"|\"$", "") :
                                String.valueOf(lit.getValue());
                        return new J.FieldAccess(
                                lit.getId(), lit.getPrefix(), lit.getMarkers(),
                                new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY),
                                JLeftPadded.build(new J.Identifier(
                                        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                                        Collections.emptyList(), name, null, null)),
                                null);
                    });
            importStmt = importStmt.withQualid(qualid);

            importStmt = importStmt.getPadding().withAlias(
                    q.receive(importStmt.getPadding().getAlias(), a -> visitLeftPadded(a, q)));
            return importStmt;
        }
    }
}
