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
package org.openrewrite.golang.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.golang.tree.Go.CompilationUnit} whose package did not type-check completely,
 * so a recipe can tell an absent type from one it merely could not see. {@code reason} names what was lost:
 * an import that would not resolve, or what ended the check early.
 */
@Value
@With
public class PartialTypeAttribution implements Marker, RpcCodec<PartialTypeAttribution> {
    UUID id;
    String reason;

    @Override
    public void rpcSend(PartialTypeAttribution after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, PartialTypeAttribution::getReason);
    }

    @Override
    public PartialTypeAttribution rpcReceive(PartialTypeAttribution before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withReason(q.receive(before.getReason()));
    }
}
