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
package org.openrewrite.javascript.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.UUID;

/**
 * What the TypeScript checker resolves each name of a compilation unit to, as a canonical name that a
 * reference and its declaration share.
 */
@Value
@With
public class ResolvedNames implements Marker, RpcCodec<ResolvedNames> {
    UUID id;

    /**
     * Each canonical name once.
     */
    List<String> names;

    /**
     * Parallel to {@link #names}: the npm package that declares each, or null for the repository's own
     * and for the default library.
     */
    List<@Nullable String> packages;

    /**
     * Each symbol kind used once.
     */
    List<String> kinds;

    /**
     * Four numbers per row: its start and end offset into the text as it prints, in UTF-16 code units,
     * an index into {@link #names} and an index into {@link #kinds}.
     */
    int[] spans;

    @Override
    public void rpcSend(ResolvedNames after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, ResolvedNames::getNames);
        q.getAndSend(after, ResolvedNames::getPackages);
        q.getAndSend(after, ResolvedNames::getKinds);
        q.getAndSend(after, ResolvedNames::getSpans);
    }

    @Override
    public ResolvedNames rpcReceive(ResolvedNames before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withNames(q.receive(before.names))
                .withPackages(q.receive(before.packages))
                .withKinds(q.receive(before.kinds))
                .withSpans(q.receiveAndGet(before.spans, ResolvedNames::toArray));
    }

    private static int[] toArray(List<Integer> spans) {
        return spans.stream().mapToInt(Integer::intValue).toArray();
    }
}
