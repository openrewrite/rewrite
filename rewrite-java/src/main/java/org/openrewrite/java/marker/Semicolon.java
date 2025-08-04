/*
 * Copyright 2023 the original author or authors.
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

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Represents optional semi-colons on java like languages.
 */
@Value
@With
public class Semicolon implements Marker, RpcCodec<Semicolon> {
    UUID id;

    @Override
    public void rpcSend(Semicolon after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public Semicolon rpcReceive(Semicolon before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
