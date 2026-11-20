/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker on a scalar, mapping or sequence whose tag was written before its anchor, as in {@code !!str &a value},
 * so that it prints back in that order.
 */
@Value
@With
public class TagBeforeAnchor implements Marker, RpcCodec<TagBeforeAnchor> {
    UUID id;

    @Override
    public void rpcSend(TagBeforeAnchor after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public TagBeforeAnchor rpcReceive(TagBeforeAnchor before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
