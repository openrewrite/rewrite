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
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.getValueNonNull;

public class GolangSender extends GolangVisitor<RpcSendQueue> {
    private final GolangSenderDelegate delegate = new GolangSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
        if (tree instanceof Go) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, J::getPrefix, space -> visitSpace(space, q));
        q.getAndSend(j, Tree::getMarkers);
        return j;
    }

    @Override
    public J visitGoCompilationUnit(Go.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, Go.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, Go.CompilationUnit::getChecksum);
        q.getAndSend(cu, Go.CompilationUnit::getFileAttributes);
        q.getAndSend(cu, c -> c.getPadding().getPackageDecl(), el -> visitRightPadded(el, q));
        q.getAndSend(cu, Go.CompilationUnit::getImportsContainer, c -> visitContainer(c, q));
        q.getAndSendList(cu, c -> c.getPadding().getStatements(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, Go.CompilationUnit::getEof, space -> visitSpace(space, q));
        return cu;
    }

    @Override
    public J visitGoStatement(Go.GoStatement goStmt, RpcSendQueue q) {
        q.getAndSend(goStmt, Go.GoStatement::getExpression, el -> visit(el, q));
        return goStmt;
    }

    @Override
    public J visitDefer(Go.Defer defer, RpcSendQueue q) {
        q.getAndSend(defer, Go.Defer::getExpression, el -> visit(el, q));
        return defer;
    }

    @Override
    public J visitSend(Go.Send send, RpcSendQueue q) {
        q.getAndSend(send, Go.Send::getChannelExpr, el -> visit(el, q));
        q.getAndSend(send, s -> s.getPadding().getArrow(), el -> visitLeftPadded(el, q));
        return send;
    }

    @Override
    public J visitGoto(Go.Goto gotoStmt, RpcSendQueue q) {
        q.getAndSend(gotoStmt, Go.Goto::getLabelIdent, el -> visit(el, q));
        return gotoStmt;
    }

    @Override
    public J visitFallthrough(Go.Fallthrough fallthrough, RpcSendQueue q) {
        return fallthrough;
    }

    @Override
    public J visitComposite(Go.Composite composite, RpcSendQueue q) {
        q.getAndSend(composite, Go.Composite::getTypeExpr, el -> visit(el, q));
        q.getAndSend(composite, c -> c.getPadding().getElements(), el -> visitContainer(el, q));
        return composite;
    }

    @Override
    public J visitKeyValue(Go.KeyValue keyValue, RpcSendQueue q) {
        q.getAndSend(keyValue, Go.KeyValue::getKeyExpr, el -> visit(el, q));
        q.getAndSend(keyValue, kv -> kv.getPadding().getValue(), el -> visitLeftPadded(el, q));
        return keyValue;
    }

    @Override
    public J visitSliceExpr(Go.SliceExpr slice, RpcSendQueue q) {
        q.getAndSend(slice, Go.SliceExpr::getIndexed, el -> visit(el, q));
        q.getAndSend(slice, Go.SliceExpr::getOpenBracket, space -> visitSpace(space, q));
        q.getAndSend(slice, s -> s.getPadding().getLow(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, s -> s.getPadding().getHigh(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, Go.SliceExpr::getMax, el -> visit(el, q));
        q.getAndSend(slice, Go.SliceExpr::getCloseBracket, space -> visitSpace(space, q));
        return slice;
    }

    @Override
    public J visitGoArrayType(Go.ArrayType arrayType, RpcSendQueue q) {
        q.getAndSend(arrayType, a -> a.getPadding().getLength(), el -> visitRightPadded(el, q));
        q.getAndSend(arrayType, Go.ArrayType::getElementType, el -> visit(el, q));
        return arrayType;
    }

    @Override
    public J visitMapType(Go.MapType mapType, RpcSendQueue q) {
        q.getAndSend(mapType, Go.MapType::getOpenBracket, space -> visitSpace(space, q));
        q.getAndSend(mapType, m -> m.getPadding().getKey(), el -> visitRightPadded(el, q));
        q.getAndSend(mapType, Go.MapType::getValue, el -> visit(el, q));
        return mapType;
    }

    @Override
    public J visitStatementExpression(Go.StatementExpression se, RpcSendQueue q) {
        q.getAndSend(se, Go.StatementExpression::getStatement, el -> visit(el, q));
        return se;
    }

    @Override
    public J visitPointerType(Go.PointerType pointerType, RpcSendQueue q) {
        q.getAndSend(pointerType, Go.PointerType::getElem, el -> visit(el, q));
        return pointerType;
    }

    @Override
    public J visitChannel(Go.Channel channel, RpcSendQueue q) {
        q.getAndSend(channel, c -> c.getDir().name());
        q.getAndSend(channel, Go.Channel::getValue, el -> visit(el, q));
        return channel;
    }

    @Override
    public J visitFuncType(Go.FuncType funcType, RpcSendQueue q) {
        q.getAndSend(funcType, f -> f.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(funcType, Go.FuncType::getReturnType, el -> visit(el, q));
        return funcType;
    }

    @Override
    public J visitStructType(Go.StructType structType, RpcSendQueue q) {
        q.getAndSend(structType, Go.StructType::getBody, el -> visit(el, q));
        return structType;
    }

    @Override
    public J visitInterfaceType(Go.InterfaceType interfaceType, RpcSendQueue q) {
        q.getAndSend(interfaceType, Go.InterfaceType::getBody, el -> visit(el, q));
        return interfaceType;
    }

    @Override
    public J visitTypeList(Go.TypeList typeList, RpcSendQueue q) {
        q.getAndSend(typeList, t -> t.getPadding().getTypes(), el -> visitContainer(el, q));
        return typeList;
    }

    @Override
    public J visitUnion(Go.Union union, RpcSendQueue q) {
        q.getAndSendList(union, u -> u.getPadding().getTypes(), t -> t.getElement().getId(), t -> visitRightPadded(t, q));
        return union;
    }

    @Override
    public J visitUnderlyingType(Go.UnderlyingType underlyingType, RpcSendQueue q) {
        q.getAndSend(underlyingType, Go.UnderlyingType::getElement, el -> visit(el, q));
        return underlyingType;
    }

    @Override
    public J visitTypeDecl(Go.TypeDecl typeDecl, RpcSendQueue q) {
        q.getAndSendList(typeDecl, Go.TypeDecl::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(typeDecl, Go.TypeDecl::getName, el -> visit(el, q));
        q.getAndSend(typeDecl, Go.TypeDecl::getTypeParameters, el -> visit(el, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getAssign(), el -> visitLeftPadded(el, q));
        q.getAndSend(typeDecl, Go.TypeDecl::getDefinition, el -> visit(el, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getSpecs(), el -> visitContainer(el, q));
        return typeDecl;
    }

    @Override
    public J visitDeclarationBlock(Go.DeclarationBlock declarationBlock, RpcSendQueue q) {
        q.getAndSendList(declarationBlock, Go.DeclarationBlock::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(declarationBlock, d -> d.getKind().name());
        q.getAndSend(declarationBlock, d -> d.getPadding().getSpecs(), el -> visitContainer(el, q));
        return declarationBlock;
    }

    @Override
    public J visitMultiAssignment(Go.MultiAssignment multiAssignment, RpcSendQueue q) {
        q.getAndSendList(multiAssignment, m -> m.getPadding().getVariables(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));
        q.getAndSend(multiAssignment, m -> m.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSendList(multiAssignment, m -> m.getPadding().getValues(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));
        return multiAssignment;
    }

    @Override
    public J visitGoReturn(Go.Return aReturn, RpcSendQueue q) {
        q.getAndSendList(aReturn, r -> r.getPadding().getExpressions(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));
        return aReturn;
    }

    @Override
    public J visitGoMethodDeclaration(Go.MethodDeclaration methodDeclaration, RpcSendQueue q) {
        q.getAndSend(methodDeclaration, m -> m.getPadding().getReceiver(), el -> visitContainer(el, q));
        q.getAndSend(methodDeclaration, Go.MethodDeclaration::getDeclaration, el -> visit(el, q));
        return methodDeclaration;
    }

    @Override
    public J visitStatementWithInit(Go.StatementWithInit statementWithInit, RpcSendQueue q) {
        q.getAndSend(statementWithInit, s -> s.getPadding().getInit(), el -> visitRightPadded(el, q));
        q.getAndSend(statementWithInit, Go.StatementWithInit::getStatement, el -> visit(el, q));
        return statementWithInit;
    }

    @Override
    public J visitCommClause(Go.CommClause commClause, RpcSendQueue q) {
        q.getAndSend(commClause, Go.CommClause::getComm, el -> visit(el, q));
        q.getAndSend(commClause, Go.CommClause::getColon, space -> visitSpace(space, q));
        q.getAndSendList(commClause, c -> c.getPadding().getBody(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        return commClause;
    }

    @Override
    public J visitIndexList(Go.IndexList indexList, RpcSendQueue q) {
        q.getAndSend(indexList, Go.IndexList::getTarget, el -> visit(el, q));
        q.getAndSend(indexList, i -> i.getPadding().getIndices(), el -> visitContainer(el, q));
        return indexList;
    }

    @Override
    public J visitGoUnary(Go.Unary unary, RpcSendQueue q) {
        q.getAndSend(unary, u -> u.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(unary, Go.Unary::getExpression, el -> visit(el, q));
        return unary;
    }

    @Override
    public J visitGoBinary(Go.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, Go.Binary::getLeft, el -> visit(el, q));
        q.getAndSend(binary, b -> b.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(binary, Go.Binary::getRight, el -> visit(el, q));
        return binary;
    }

    @Override
    public J visitGoAssignmentOperation(Go.AssignmentOperation assignOp, RpcSendQueue q) {
        q.getAndSend(assignOp, Go.AssignmentOperation::getVariable, el -> visit(el, q));
        q.getAndSend(assignOp, a -> a.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(assignOp, Go.AssignmentOperation::getAssignment, el -> visit(el, q));
        return assignOp;
    }

    @Override
    public J visitGoVariadic(Go.Variadic variadic, RpcSendQueue q) {
        q.getAndSend(variadic, Go.Variadic::getElement, el -> visit(el, q));
        q.getAndSend(variadic, Go.Variadic::getDots, space -> visitSpace(space, q));
        q.getAndSend(variadic, Go.Variadic::isPostfix);
        return variadic;
    }

    // Delegation methods to JavaSender for RPC-specific visit methods
    public <T> void visitLeftPadded(JLeftPadded<T> left, RpcSendQueue q) {
        delegate.visitLeftPadded(left, q);
    }

    public <T> void visitRightPadded(JRightPadded<T> right, RpcSendQueue q) {
        delegate.visitRightPadded(right, q);
    }

    public <J2 extends J> void visitContainer(JContainer<J2> container, RpcSendQueue q) {
        delegate.visitContainer(container, q);
    }

    public void visitSpace(Space space, RpcSendQueue q) {
        delegate.visitSpace(space, q);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcSendQueue q) {
        return delegate.visitType(javaType, q);
    }

    private static class GolangSenderDelegate extends JavaSender {
        private final GolangSender delegate;

        public GolangSenderDelegate(GolangSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
            if (tree instanceof Go) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public J visitImport(J.Import importStmt, RpcSendQueue q) {
            q.getAndSend(importStmt, i -> i.getPadding().getStatic(), s -> visitLeftPadded(s, q));
            // Convert FieldAccess qualid to Literal for Go
            q.getAndSend(importStmt, i -> {
                J.FieldAccess fa = i.getQualid();
                String name = fa.getSimpleName();
                return new J.Literal(fa.getId(), fa.getPrefix(), fa.getMarkers(),
                        name, "\"" + name + "\"", null, JavaType.Primitive.String);
            }, id -> visit(id, q));
            q.getAndSend(importStmt, i -> i.getPadding().getAlias(), alias -> visitLeftPadded(alias, q));
            return importStmt;
        }
    }
}
