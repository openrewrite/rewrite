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
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.zig.ZigVisitor;
import org.openrewrite.zig.tree.Zig;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ZigReceiver extends ZigVisitor<RpcReceiveQueue> {
    private final ZigReceiverDelegate delegate = new ZigReceiverDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
        if (tree instanceof Zig) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        return ((J) j.withId(q.receiveAndGet(j.getId(), UUID::fromString)))
                .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public J visitZigCompilationUnit(Zig.CompilationUnit cu, RpcReceiveQueue q) {
        return cu.withSourcePath(q.<Path, String>receiveAndGet(cu.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(cu.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<Zig.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .withImportsContainer(q.receive(cu.getImportsContainer(), c -> visitContainer(c, q)))
                .getPadding().withStatements(q.receiveList(cu.getPadding().getStatements(), stmt -> visitRightPadded(stmt, q)))
                .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitComptime(Zig.Comptime comptime, RpcReceiveQueue q) {
        return comptime
                .withExpression(q.receive(comptime.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitDefer(Zig.Defer defer, RpcReceiveQueue q) {
        return defer
                .withErrdefer(q.receive(defer.isErrdefer()))
                .withPayload(q.receive(defer.getPayload(), el -> (Zig.Payload) visitNonNull(el, q)))
                .withExpression(q.receive(defer.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitTestDecl(Zig.TestDecl testDecl, RpcReceiveQueue q) {
        return testDecl
                .withName(q.receive(testDecl.getName(), el -> (J.Literal) visitNonNull(el, q)))
                .withBody(q.receive(testDecl.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitBuiltinCall(Zig.BuiltinCall builtinCall, RpcReceiveQueue q) {
        return builtinCall
                .withName(q.receive(builtinCall.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withArguments(q.receive(builtinCall.getPadding().getArguments(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitPayload(Zig.Payload payload, RpcReceiveQueue q) {
        return payload
                .getPadding().withNames(q.receive(payload.getPadding().getNames(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitErrorUnion(Zig.ErrorUnion errorUnion, RpcReceiveQueue q) {
        return errorUnion
                .withErrorType(q.receive(errorUnion.getErrorType(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withValueType(q.receive(errorUnion.getPadding().getValueType(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitOptional(Zig.Optional optional, RpcReceiveQueue q) {
        return optional
                .withValueType(q.receive(optional.getValueType(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitSlice(Zig.Slice slice, RpcReceiveQueue q) {
        return slice
                .withTarget(q.receive(slice.getTarget(), el -> (Expression) visitNonNull(el, q)))
                .withOpenBracket(q.receive(slice.getOpenBracket(), space -> visitSpace(space, q)))
                .getPadding().withStart(q.receive(slice.getPadding().getStart(), el -> visitRightPadded(el, q)))
                .withEnd(q.receive(slice.getEnd(), el -> (Expression) visitNonNull(el, q)))
                .withCloseBracket(q.receive(slice.getCloseBracket(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitSwitchProng(Zig.SwitchProng switchProng, RpcReceiveQueue q) {
        return switchProng
                .getPadding().withCases(q.receive(switchProng.getPadding().getCases(), el -> visitContainer(el, q)))
                .withPayload(q.receive(switchProng.getPayload(), el -> (Zig.Payload) visitNonNull(el, q)))
                .getPadding().withArrow(q.receive(switchProng.getPadding().getArrow(), el -> visitLeftPadded(el, q)));
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

    private static class ZigReceiverDelegate extends JavaReceiver {
        private final ZigReceiver delegate;

        public ZigReceiverDelegate(ZigReceiver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
            if (tree instanceof Zig) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public J visitUnknownSource(J.Unknown.Source source, RpcReceiveQueue q) {
            return source.withText(q.receive(source.getText()));
        }
    }
}
