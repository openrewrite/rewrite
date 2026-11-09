/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

import static org.openrewrite.java.internal.rpc.JavaSender.spaceRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

@Value
@With
public class Struct implements Marker, RpcCodec<Struct> {
    UUID id;

    /**
     * The space between {@code record} and {@code struct}.
     */
    @Nullable
    Space prefix;

    @JsonCreator
    public Struct(@JsonProperty("id") UUID id, @JsonProperty("prefix") @Nullable Space prefix) {
        this.id = id;
        this.prefix = prefix;
    }

    public Struct(UUID id) {
        this(id, Space.EMPTY);
    }

    public Space getPrefix() {
        return prefix == null ? Space.EMPTY : prefix;
    }

    public Struct withPrefix(Space prefix) {
        return getPrefix() == prefix ? this : new Struct(id, prefix);
    }

    @Override
    public void rpcSend(Struct after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, x -> spaceRef(x.getPrefix()), space -> new JavaSender().visitSpace(getValueNonNull(space), q));
    }

    @Override
    public Struct rpcReceive(Struct before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withPrefix(q.receive(before.getPrefix(), space -> new JavaReceiver().visitSpace(space, q)));
    }
}
