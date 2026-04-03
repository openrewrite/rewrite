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
    public J visitTypeDecl(Go.TypeDecl typeDecl, RpcSendQueue q) {
        q.getAndSend(typeDecl, Go.TypeDecl::getName, el -> visit(el, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getAssign(), el -> visitLeftPadded(el, q));
        q.getAndSend(typeDecl, Go.TypeDecl::getDefinition, el -> visit(el, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getSpecs(), el -> visitContainer(el, q));
        return typeDecl;
    }

    @Override
    public J visitMultiAssignment(Go.MultiAssignment multiAssignment, RpcSendQueue q) {
        q.getAndSendList(multiAssignment, m -> m.getPadding().getVariables(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));
        q.getAndSend(multiAssignment, m -> m.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSendList(multiAssignment, m -> m.getPadding().getValues(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));
        return multiAssignment;
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
        public J visitForEachControl(J.ForEachLoop.Control control, RpcSendQueue q) {
            // Send in Go's format: key (right-padded), value (right-padded), operator (left-padded string), iterable
            // Extract key identifier from variable declarations
            Statement varStmt = control.getVariable();
            JRightPadded<Expression> key = null;
            if (varStmt instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecls = (J.VariableDeclarations) varStmt;
                if (!varDecls.getVariables().isEmpty()) {
                    J.VariableDeclarations.NamedVariable nv = varDecls.getVariables().get(0);
                    key = JRightPadded.<Expression>build(nv.getName()).withAfter(control.getPadding().getVariable().getAfter());
                }
            }
            final JRightPadded<Expression> finalKey = key;
            // key
            q.getAndSend(control, c -> finalKey, el -> visitRightPadded(el, q));
            // value (null for Go's single-variable range)
            q.getAndSend(control, c -> (JRightPadded<Expression>) null, el -> visitRightPadded(el, q));
            // operator (left-padded string - ":=")
            q.getAndSend(control, c -> JLeftPadded.build(":=").withBefore(Space.EMPTY));
            // iterable
            q.getAndSend(control, c -> c.getIterable(), el -> visit(el, q));
            return control;
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
