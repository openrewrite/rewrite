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
package org.openrewrite.csharp.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker on an ArrayDimension indicating it is a continuation within the same
 * rank specifier (e.g., the second dimension of new int[n, m]).
 * The printer uses this to emit a comma instead of opening a new bracket pair.
 */
@Value
@With
public class MultiDimensionContinuation implements Marker, RpcCodec<MultiDimensionContinuation> {
    UUID id;

    @Override
    public void rpcSend(MultiDimensionContinuation after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public MultiDimensionContinuation rpcReceive(MultiDimensionContinuation before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
