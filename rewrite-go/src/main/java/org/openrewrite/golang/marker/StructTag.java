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
package org.openrewrite.golang.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

@Value
@With
public class StructTag implements Marker, RpcCodec<StructTag> {
    UUID id;
    J.Literal tag;

    @Override
    public void rpcSend(StructTag after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, a -> a.getTag().getValueSource());
    }

    @Override
    public StructTag rpcReceive(StructTag before, RpcReceiveQueue q) {
        UUID id = q.receiveAndGet(before.getId(), UUID::fromString);
        String valueSource = q.<String, String>receiveAndGet(
                before.getTag() != null ? before.getTag().getValueSource() : null, s -> s);
        J.Literal tag;
        if (before.getTag() != null) {
            tag = before.getTag().withValueSource(valueSource);
        } else if (valueSource != null) {
            tag = new J.Literal(java.util.UUID.randomUUID(),
                    org.openrewrite.java.tree.Space.EMPTY, org.openrewrite.marker.Markers.EMPTY,
                    valueSource, valueSource, null, org.openrewrite.java.tree.JavaType.Primitive.String);
        } else {
            tag = null;
        }
        return before.withId(id).withTag(tag);
    }
}
