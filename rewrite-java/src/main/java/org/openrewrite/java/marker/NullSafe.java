/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.marker;

import lombok.EqualsAndHashCode;
import lombok.Getter;
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
 * Marker indicating null-safe navigation (?. operator).
 * Used by languages with safe navigation: C#, Kotlin, Groovy.
 * When present on a MethodInvocation.name or FieldAccess.name, prints ?. instead of .
 */
@EqualsAndHashCode
@Getter
@With
public class NullSafe implements Marker, RpcCodec<NullSafe> {
    private final UUID id;

    /**
     * Whitespace between '?' and '.' when they are separated (e.g., by a newline).
     */
    private final Space dotPrefix;

    public NullSafe(UUID id) {
        this(id, Space.EMPTY);
    }

    public NullSafe(UUID id, Space dotPrefix) {
        this.id = id;
        this.dotPrefix = dotPrefix != null ? dotPrefix : Space.EMPTY;
    }

    @Override
    public void rpcSend(NullSafe after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, NullSafe::getDotPrefix, space -> new JavaSender().visitSpace(space, q));
    }

    @Override
    public NullSafe rpcReceive(NullSafe before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withDotPrefix(q.receive(before.getDotPrefix(), space -> new JavaReceiver().visitSpace(space, q)));
    }
}
