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
 * Marks a {@link org.openrewrite.java.tree.J.MethodInvocation} whose callee is a type rather than a function —
 * {@code []byte(s)}, {@code string(b)}, {@code MyInt(3)}. Go spells a conversion exactly like a call and there is
 * no method to attribute, so the marker is what identifies one.
 */
@Value
@With
public class Conversion implements Marker, RpcCodec<Conversion> {
    UUID id;

    @Override
    public void rpcSend(Conversion after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public Conversion rpcReceive(Conversion before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
