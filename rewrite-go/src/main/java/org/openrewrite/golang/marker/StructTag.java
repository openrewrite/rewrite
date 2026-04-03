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
        // Send the tag literal's value as a simple string instead of a tree node.
        // The tree node doesn't round-trip cleanly through the raw-value path.
        q.getAndSend(after, st -> st.getTag() != null ? st.getTag().getValueSource() : null);
    }

    @Override
    public StructTag rpcReceive(StructTag before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withTag(q.<J.Literal, Object>receiveAndGet(before.getTag(), valueSource -> {
                    if (valueSource == null) return null;
                    String vs = valueSource.toString();
                    return new J.Literal(
                            before.getTag() != null ? before.getTag().getId() : java.util.UUID.randomUUID(),
                            before.getTag() != null ? before.getTag().getPrefix() : org.openrewrite.java.tree.Space.EMPTY,
                            before.getTag() != null ? before.getTag().getMarkers() : org.openrewrite.marker.Markers.EMPTY,
                            vs,
                            vs,
                            null,
                            org.openrewrite.java.tree.JavaType.Primitive.String
                    );
                }));
    }
}
