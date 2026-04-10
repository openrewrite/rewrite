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
package org.openrewrite.zig.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.zig.ZigVisitor;
import org.openrewrite.zig.tree.Zig;
import org.openrewrite.rpc.RpcSendQueue;

public class ZigSender extends ZigVisitor<RpcSendQueue> {
    private final ZigSenderDelegate delegate = new ZigSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
        if (tree instanceof Zig) {
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
    public J visitZigCompilationUnit(Zig.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, Zig.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, Zig.CompilationUnit::getChecksum);
        q.getAndSend(cu, Zig.CompilationUnit::getFileAttributes);
        q.getAndSend(cu, Zig.CompilationUnit::getImportsContainer, c -> visitContainer(c, q));
        q.getAndSendList(cu, c -> c.getPadding().getStatements(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, Zig.CompilationUnit::getEof, space -> visitSpace(space, q));
        return cu;
    }

    @Override
    public J visitComptime(Zig.Comptime comptime, RpcSendQueue q) {
        q.getAndSend(comptime, Zig.Comptime::getExpression, el -> visit(el, q));
        return comptime;
    }

    @Override
    public J visitDefer(Zig.Defer defer, RpcSendQueue q) {
        q.getAndSend(defer, Zig.Defer::isErrdefer);
        q.getAndSend(defer, Zig.Defer::getPayload, el -> visit(el, q));
        q.getAndSend(defer, Zig.Defer::getExpression, el -> visit(el, q));
        return defer;
    }

    @Override
    public J visitTestDecl(Zig.TestDecl testDecl, RpcSendQueue q) {
        q.getAndSend(testDecl, Zig.TestDecl::getName, el -> visit(el, q));
        q.getAndSend(testDecl, Zig.TestDecl::getBody, el -> visit(el, q));
        return testDecl;
    }

    @Override
    public J visitBuiltinCall(Zig.BuiltinCall builtinCall, RpcSendQueue q) {
        q.getAndSend(builtinCall, Zig.BuiltinCall::getName, el -> visit(el, q));
        q.getAndSend(builtinCall, b -> b.getPadding().getArguments(), el -> visitContainer(el, q));
        return builtinCall;
    }

    @Override
    public J visitPayload(Zig.Payload payload, RpcSendQueue q) {
        q.getAndSend(payload, pl -> pl.getPadding().getNames(), el -> visitContainer(el, q));
        return payload;
    }

    @Override
    public J visitErrorUnion(Zig.ErrorUnion errorUnion, RpcSendQueue q) {
        q.getAndSend(errorUnion, Zig.ErrorUnion::getErrorType, el -> visit(el, q));
        q.getAndSend(errorUnion, e -> e.getPadding().getValueType(), el -> visitLeftPadded(el, q));
        return errorUnion;
    }

    @Override
    public J visitOptional(Zig.Optional optional, RpcSendQueue q) {
        q.getAndSend(optional, Zig.Optional::getValueType, el -> visit(el, q));
        return optional;
    }

    @Override
    public J visitSlice(Zig.Slice slice, RpcSendQueue q) {
        q.getAndSend(slice, Zig.Slice::getTarget, el -> visit(el, q));
        q.getAndSend(slice, Zig.Slice::getOpenBracket, space -> visitSpace(space, q));
        q.getAndSend(slice, s -> s.getPadding().getStart(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, Zig.Slice::getEnd, el -> visit(el, q));
        q.getAndSend(slice, Zig.Slice::getCloseBracket, space -> visitSpace(space, q));
        return slice;
    }

    @Override
    public J visitSwitchProng(Zig.SwitchProng switchProng, RpcSendQueue q) {
        q.getAndSend(switchProng, sp -> sp.getPadding().getCases(), el -> visitContainer(el, q));
        q.getAndSend(switchProng, Zig.SwitchProng::getPayload, el -> visit(el, q));
        q.getAndSend(switchProng, sp -> sp.getPadding().getArrow(), el -> visitLeftPadded(el, q));
        return switchProng;
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

    private static class ZigSenderDelegate extends JavaSender {
        private final ZigSender delegate;

        public ZigSenderDelegate(ZigSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
            if (tree instanceof Zig) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public J visitUnknownSource(J.Unknown.Source source, RpcSendQueue q) {
            q.getAndSend(source, J.Unknown.Source::getText);
            return source;
        }
    }
}
