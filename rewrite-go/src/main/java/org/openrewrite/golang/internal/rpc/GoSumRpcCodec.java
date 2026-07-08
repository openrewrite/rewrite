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
import org.openrewrite.golang.tree.GoSum;
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
 * RPC codec for the {@link GoSum} SourceFile. The field order here is the single
 * source of truth shared with the Go-side {@code sendGoSum}/{@code receiveGoSum}
 * (pkg/rpc/gosum_codec.go) — both must agree exactly or the cross-language queue
 * desyncs.
 * <p>
 * Like {@link GoModRpcCodec}, this serializes go.sum's bespoke node set manually
 * rather than via the J-element padding helpers.
 */
@Getter
public class GoSumRpcCodec extends DynamicDispatchRpcCodec<GoSum> {

    @Override
    public String getSourceFileType() {
        return GoSum.class.getName();
    }

    @Override
    public Class<? extends GoSum> getType() {
        return GoSum.class;
    }

    @Override
    public void rpcSend(GoSum after, RpcSendQueue q) {
        GolangSender sender = new GolangSender();
        q.getAndSend(after, Tree::getId);
        q.getAndSend(after, GoSum::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(after, Tree::getMarkers);
        q.getAndSend(after, (GoSum g) -> g.getSourcePath().toString());
        q.getAndSend(after, (GoSum g) -> g.getCharset().name());
        q.getAndSend(after, GoSum::isCharsetBomMarked);
        q.getAndSend(after, GoSum::getChecksum);
        q.getAndSend(after, GoSum::getFileAttributes);
        q.getAndSendList(after, GoSum::getLines,
                rp -> rp.getElement().getId(),
                rp -> sendRightPadded(sender, rp, q));
        q.getAndSend(after, GoSum::getEof, space -> sender.visitSpace(space, q));
    }

    private static void sendRightPadded(GolangSender sender, JRightPadded<GoSum.Line> rp, RpcSendQueue q) {
        q.getAndSend(rp, JRightPadded::getElement, el -> sendLine(sender, el, q));
        q.getAndSend(rp, JRightPadded::getAfter, space -> sender.visitSpace(space, q));
        q.getAndSend(rp, JRightPadded::getMarkers);
    }

    private static void sendLine(GolangSender sender, GoSum.Line l, RpcSendQueue q) {
        q.getAndSend(l, GoSum.Line::getId);
        q.getAndSend(l, GoSum.Line::getPrefix, space -> sender.visitSpace(space, q));
        q.getAndSend(l, GoSum.Line::getMarkers);
        q.getAndSend(l, GoSum.Line::getModulePath);
        q.getAndSend(l, GoSum.Line::getVersion);
        q.getAndSend(l, GoSum.Line::isGoMod);
        q.getAndSend(l, GoSum.Line::getHash);
    }

    @Override
    public GoSum rpcReceive(GoSum before, RpcReceiveQueue q) {
        GolangReceiver receiver = new GolangReceiver();
        GoSum t = before;
        t = t.withId(q.receiveAndGet(t.getId(), UUID::fromString));
        t = t.withPrefix(q.receive(t.getPrefix(), space -> receiver.visitSpace(space, q)));
        t = t.withMarkers(q.receive(t.getMarkers()));
        t = t.withSourcePath(q.<Path, String>receiveAndGet(t.getSourcePath(), Paths::get));
        t = (GoSum) t.withCharset(q.<Charset, String>receiveAndGet(t.getCharset(), Charset::forName));
        t = t.withCharsetBomMarked(q.receive(t.isCharsetBomMarked()));
        t = t.withChecksum(q.receive(t.getChecksum()));
        t = t.withFileAttributes(q.receive(t.getFileAttributes()));
        t = t.withLines(q.receiveList(t.getLines(), rp -> receiveRightPadded(receiver, rp, q)));
        t = t.withEof(q.receive(t.getEof(), space -> receiver.visitSpace(space, q)));
        return t;
    }

    private static JRightPadded<GoSum.Line> receiveRightPadded(GolangReceiver receiver,
                                                               @Nullable JRightPadded<GoSum.Line> rp,
                                                               RpcReceiveQueue q) {
        GoSum.Line beforeElem = rp == null ? null : rp.getElement();
        GoSum.Line elem = q.receive(beforeElem, el -> receiveLine(receiver, el, q));
        Space after = q.receive(rp == null ? null : rp.getAfter(), space -> receiver.visitSpace(space, q));
        Markers markers = q.receive(rp == null ? null : rp.getMarkers());
        return new JRightPadded<>(elem, after, markers);
    }

    private static GoSum.Line receiveLine(GolangReceiver receiver, GoSum.Line l, RpcReceiveQueue q) {
        return l.withId(q.receiveAndGet(l.getId(), UUID::fromString))
                .withPrefix(q.receive(l.getPrefix(), space -> receiver.visitSpace(space, q)))
                .withMarkers(q.receive(l.getMarkers()))
                .withModulePath(q.receive(l.getModulePath()))
                .withVersion(q.receive(l.getVersion()))
                .withGoMod(q.receive(l.isGoMod()))
                .withHash(q.receive(l.getHash()));
    }
}
