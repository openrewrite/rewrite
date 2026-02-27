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
 * Marker on a ConstrainedTypeParameter that records the source-order index
 * of its where clause, so the printer can output constraints in source order.
 */
@Value
@With
public class WhereClauseOrder implements Marker, RpcCodec<WhereClauseOrder> {
    UUID id;
    int order;

    @Override
    public void rpcSend(WhereClauseOrder after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, WhereClauseOrder::getOrder);
    }

    @Override
    public WhereClauseOrder rpcReceive(WhereClauseOrder before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withOrder(q.receive(before.getOrder()));
    }
}
