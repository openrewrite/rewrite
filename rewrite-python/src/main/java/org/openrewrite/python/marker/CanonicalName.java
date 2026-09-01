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
 * The FQN of the symbol a {@code J.Import} binds, at the module defining it, when the module the
 * source imported from re-exports it: {@code from os.path import join} binds {@code posixpath.join}.
 * Types name the module the source wrote, so this is where an import's identity across spellings lives.
 */
@Value
@With
public class CanonicalName implements Marker, RpcCodec<CanonicalName> {
    UUID id;
    String fqn;

    @Override
    public void rpcSend(CanonicalName after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, CanonicalName::getFqn);
    }

    @Override
    public CanonicalName rpcReceive(CanonicalName before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withFqn(q.receive(before.getFqn()));
    }
}
