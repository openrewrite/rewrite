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
package org.openrewrite.python.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker indicating that a {@code J.Throw} represents a Python 2 three-argument
 * {@code raise E, v, tb} form.
 * <p>
 * The exception slot of the {@code J.Throw} holds a {@code Py.CollectionLiteral} of
 * kind {@code TUPLE} containing the three operands; when this marker is present the
 * printer drops the tuple's enclosing parentheses and renders the legacy comma-
 * separated syntax so Python 2 sources round-trip byte-for-byte.
 */
@Value
@With
public class RaiseTuple implements Marker, RpcCodec<RaiseTuple> {
    UUID id;

    @Override
    public void rpcSend(RaiseTuple after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public RaiseTuple rpcReceive(RaiseTuple before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
