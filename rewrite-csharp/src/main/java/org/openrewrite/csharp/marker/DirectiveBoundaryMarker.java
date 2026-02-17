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

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.UUID;

/**
 * Marks a tree node that is adjacent to one or more conditional preprocessor directive boundaries.
 * Each directive index references a position in the {@code ConditionalDirective.directiveLines} list.
 */
@Value
@With
public class DirectiveBoundaryMarker implements Marker, RpcCodec<DirectiveBoundaryMarker> {
    UUID id;
    List<Integer> directiveIndices;

    @Override
    public void rpcSend(DirectiveBoundaryMarker after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSendList(after, m -> m.directiveIndices, Object::toString, idx ->
                q.getAndSend(idx, Object::toString));
    }

    @Override
    public DirectiveBoundaryMarker rpcReceive(DirectiveBoundaryMarker before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withDirectiveIndices(q.receiveList(before.directiveIndices, idx ->
                        q.<Integer, String>receiveAndGet(idx, Integer::parseInt)));
    }
}
