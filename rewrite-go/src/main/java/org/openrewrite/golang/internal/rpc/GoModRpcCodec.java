/*
 * Copyright 2026 the original author or authors.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoMod.GoModStatement;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * RPC codec for the {@link GoMod} SourceFile. The field order here is the single
 * source of truth shared with the Go-side {@code sendGoMod}/{@code receiveGoMod}
 * (pkg/rpc/gomod_codec.go) — both must agree exactly or the cross-language queue
 * desyncs.
 * <p>
 * Like the Go side, this codec serializes go.mod's bespoke node set manually rather
 * than via the J-element padding helpers: {@code Space} is delegated to
 * {@link GolangSender#visitSpace}/{@link GolangReceiver#visitSpace} (wire-compatible
 * with the Go {@code sendSpace}), {@code Markers} auto-dispatch through their own
 * {@code RpcCodec}, and the statement/value structure is walked explicitly.
 */
@Getter
public class GoModRpcCodec extends DynamicDispatchRpcCodec<GoMod> {

    @Override
    public String getSourceFileType() {
        return GoMod.class.getName();
    }

    @Override
    public Class<? extends GoMod> getType() {
        return GoMod.class;
    }

    @Override
    public void rpcSend(GoMod after, RpcSendQueue q) {
        GolangSender sender = new GolangSender();
        q.getAndSend(after, Tree::getId);
        q.getAndSend(after, GoMod::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(after, Tree::getMarkers);
        q.getAndSend(after, (GoMod g) -> g.getSourcePath().toString());
        q.getAndSend(after, (GoMod g) -> g.getCharset().name());
        q.getAndSend(after, GoMod::isCharsetBomMarked);
        q.getAndSend(after, GoMod::getChecksum);
        q.getAndSend(after, GoMod::getFileAttributes);
        q.getAndSendList(after, GoMod::getStatements,
                rp -> rp.getElement().getId(),
                rp -> sendRightPadded(sender, rp, q));
        q.getAndSend(after, GoMod::getEof, space -> sender.visitSpace(space, q));
    }

    private static void sendRightPadded(GolangSender sender, JRightPadded<GoModStatement> rp, RpcSendQueue q) {
        q.getAndSend(rp, JRightPadded::getElement, el -> sendStatement(sender, el, q));
        q.getAndSend(rp, JRightPadded::getAfter, space -> sender.visitSpace(space, q));
        q.getAndSend(rp, JRightPadded::getMarkers);
    }

    private static void sendStatement(GolangSender sender, GoModStatement s, RpcSendQueue q) {
        if (s instanceof GoMod.Directive) {
            sendDirective(sender, (GoMod.Directive) s, q);
        } else if (s instanceof GoMod.Block) {
            sendBlock(sender, (GoMod.Block) s, q);
        }
    }

    private static void sendDirective(GolangSender sender, GoMod.Directive d, RpcSendQueue q) {
        q.getAndSend(d, GoMod.Directive::getId);
        q.getAndSend(d, GoMod.Directive::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(d, GoMod.Directive::getMarkers);
        q.getAndSend(d, GoMod.Directive::getKeyword);
        q.getAndSendList(d, GoMod.Directive::getValues, GoMod.Value::getId, v -> sendValue(sender, v, q));
    }

    private static void sendBlock(GolangSender sender, GoMod.Block b, RpcSendQueue q) {
        q.getAndSend(b, GoMod.Block::getId);
        q.getAndSend(b, GoMod.Block::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(b, GoMod.Block::getMarkers);
        q.getAndSend(b, GoMod.Block::getKeyword);
        q.getAndSend(b, GoMod.Block::getBeforeLParen, space -> sender.visitSpace(space, q));
        q.getAndSendList(b, GoMod.Block::getEntries,
                rp -> rp.getElement().getId(),
                rp -> sendRightPadded(sender, rp, q));
        q.getAndSend(b, GoMod.Block::getBeforeRParen, space -> sender.visitSpace(space, q));
    }

    private static void sendValue(GolangSender sender, GoMod.Value v, RpcSendQueue q) {
        q.getAndSend(v, GoMod.Value::getId);
        q.getAndSend(v, GoMod.Value::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(v, GoMod.Value::getMarkers);
        q.getAndSend(v, GoMod.Value::getText);
    }

    @Override
    public GoMod rpcReceive(GoMod before, RpcReceiveQueue q) {
        GolangReceiver receiver = new GolangReceiver();
        GoMod t = before;
        t = t.withId(q.receiveAndGet(t.getId(), UUID::fromString));
        t = t.withPrefix(q.receive(t.getPrefix(), space -> receiver.visitSpace(space, q)));
        t = t.withMarkers(q.receive(t.getMarkers()));
        t = t.withSourcePath(q.<Path, String>receiveAndGet(t.getSourcePath(), Paths::get));
        t = (GoMod) t.withCharset(q.<Charset, String>receiveAndGet(t.getCharset(), Charset::forName));
        t = t.withCharsetBomMarked(q.receive(t.isCharsetBomMarked()));
        t = t.withChecksum(q.receive(t.getChecksum()));
        t = t.withFileAttributes(q.receive(t.getFileAttributes()));
        t = t.withStatements(q.receiveList(t.getStatements(), rp -> receiveRightPadded(receiver, rp, q)));
        t = t.withEof(q.receive(t.getEof(), space -> receiver.visitSpace(space, q)));
        return t;
    }

    private static JRightPadded<GoModStatement> receiveRightPadded(GolangReceiver receiver,
                                                                   @Nullable JRightPadded<GoModStatement> rp,
                                                                   RpcReceiveQueue q) {
        GoModStatement beforeElem = rp == null ? null : rp.getElement();
        GoModStatement elem = q.receive(beforeElem, el -> receiveStatement(receiver, el, q));
        Space after = q.receive(rp == null ? null : rp.getAfter(), space -> receiver.visitSpace(space, q));
        Markers markers = q.receive(rp == null ? null : rp.getMarkers());
        return new JRightPadded<>(elem, after, markers);
    }

    private static GoModStatement receiveStatement(GolangReceiver receiver, GoModStatement before, RpcReceiveQueue q) {
        if (before instanceof GoMod.Directive) {
            return receiveDirective(receiver, (GoMod.Directive) before, q);
        } else if (before instanceof GoMod.Block) {
            return receiveBlock(receiver, (GoMod.Block) before, q);
        }
        return before;
    }

    private static GoMod.Directive receiveDirective(GolangReceiver receiver, GoMod.Directive d, RpcReceiveQueue q) {
        return d.withId(q.receiveAndGet(d.getId(), UUID::fromString))
                .withPrefix(q.receive(d.getPrefix(), space -> receiver.visitSpace(space, q)))
                .withMarkers(q.receive(d.getMarkers()))
                .withKeyword(q.receive(d.getKeyword()))
                .withValues(q.receiveList(d.getValues(), v -> receiveValue(receiver, v, q)));
    }

    private static GoMod.Block receiveBlock(GolangReceiver receiver, GoMod.Block b, RpcReceiveQueue q) {
        return b.withId(q.receiveAndGet(b.getId(), UUID::fromString))
                .withPrefix(q.receive(b.getPrefix(), space -> receiver.visitSpace(space, q)))
                .withMarkers(q.receive(b.getMarkers()))
                .withKeyword(q.receive(b.getKeyword()))
                .withBeforeLParen(q.receive(b.getBeforeLParen(), space -> receiver.visitSpace(space, q)))
                .withEntries(q.receiveList(b.getEntries(), rp -> receiveRightPadded(receiver, rp, q)))
                .withBeforeRParen(q.receive(b.getBeforeRParen(), space -> receiver.visitSpace(space, q)));
    }

    private static GoMod.Value receiveValue(GolangReceiver receiver, GoMod.Value v, RpcReceiveQueue q) {
        return v.withId(q.receiveAndGet(v.getId(), UUID::fromString))
                .withPrefix(q.receive(v.getPrefix(), space -> receiver.visitSpace(space, q)))
                .withMarkers(q.receive(v.getMarkers()))
                .withText(q.receive(v.getText()));
    }
}
