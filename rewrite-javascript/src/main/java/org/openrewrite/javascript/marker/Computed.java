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
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * A name written in brackets where the model has room for an identifier only, as the member
 * of {@code enum A { ['b'] }} is. The identifier holds the bracketed literal.
 */
@Value
@With
public class Computed implements Marker, RpcCodec<Computed> {
    UUID id;

    /**
     * The space before the closing bracket.
     */
    Space suffix;

    @Override
    public void rpcSend(Computed after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, Computed::getSuffix, space -> new JavaSender().visitSpace(space, q));
    }

    @Override
    public Computed rpcReceive(Computed before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withSuffix(q.receive(before.getSuffix(), space -> new JavaReceiver().visitSpace(space, q)));
    }
}
