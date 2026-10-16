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
package org.openrewrite.csharp.rpc;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcMarker;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.StringJoiner;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * Reports the type each marker of a compilation unit arrived as, for the C# tests that send
 * markers here. Marking the tree also makes it travel back.
 */
@RequiredArgsConstructor
public class MarkerProbe extends CSharpVisitor<ExecutionContext> {

    /**
     * Whether to replace each marker Java has no type for with a copy, so that it is sent back
     * whole rather than left as it was.
     */
    private final boolean resend;

    /**
     * Whether to send back a tree that changed all over and that the C# side cannot receive.
     */
    private final boolean poison;

    @Override
    public J visitCompilationUnit(Cs.CompilationUnit cu, ExecutionContext ctx) {
        StringJoiner received = new StringJoiner(",");
        for (Object marker : cu.getMarkers().getMarkers()) {
            received.add(marker == null ? "null" : marker.getClass().getName());
        }
        if (resend) {
            cu = cu.withMarkers(cu.getMarkers().withMarkers(ListUtils.map(cu.getMarkers().getMarkers(), MarkerProbe::copy)));
        }
        if (poison) {
            cu = (Cs.CompilationUnit) super.visitCompilationUnit(cu, ctx);
            cu = cu.withMarkers(cu.getMarkers().add(new Undecodable(randomId())));
        }
        return SearchResult.found(cu, received.toString());
    }

    @Override
    public J postVisit(J tree, ExecutionContext ctx) {
        return poison && !(tree instanceof Cs.CompilationUnit) ? SearchResult.found(tree) : tree;
    }

    private static Marker copy(Marker marker) {
        if (!(marker instanceof RpcMarker)) {
            return marker;
        }
        RpcMarker copy = new RpcMarker(marker.getId());
        ((RpcMarker) marker).getData().forEach(copy::addData);
        return copy;
    }

    /**
     * A marker with a codec, and with no counterpart on the C# side to receive it.
     */
    @Value
    public static class Undecodable implements Marker, RpcCodec<Undecodable> {
        @With
        UUID id;

        @Override
        public void rpcSend(Undecodable after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public Undecodable rpcReceive(Undecodable before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }
}
