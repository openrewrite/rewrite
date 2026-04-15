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
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

@Value
@With
public class ImportBlock implements Marker, RpcCodec<ImportBlock> {
    UUID id;
    boolean closePrevious;
    Space before;
    boolean grouped;
    Space groupedBefore;

    @Override
    public void rpcSend(ImportBlock after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, ImportBlock::isClosePrevious);
        q.getAndSend(after, b -> b.getBefore().getWhitespace());
        q.getAndSend(after, ImportBlock::isGrouped);
        q.getAndSend(after, b -> b.getGroupedBefore().getWhitespace());
    }

    @Override
    public ImportBlock rpcReceive(ImportBlock before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withClosePrevious(q.receiveAndGet(before.isClosePrevious(), Boolean::parseBoolean))
                .withBefore(Space.format(q.receive(before.getBefore() == null ? "" : before.getBefore().getWhitespace())))
                .withGrouped(q.receiveAndGet(before.isGrouped(), Boolean::parseBoolean))
                .withGroupedBefore(Space.format(q.receive(before.getGroupedBefore() == null ? "" : before.getGroupedBefore().getWhitespace())));
    }
}
