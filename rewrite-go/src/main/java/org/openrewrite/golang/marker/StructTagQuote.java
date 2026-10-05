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
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Records which string literal delimiter a struct tag was written with.
 * The tag itself is modelled as leading annotations, which carry its keys
 * and values but not the quoting; absent this marker the printer writes a
 * raw string.
 */
@Value
@With
public class StructTagQuote implements Marker, RpcCodec<StructTagQuote> {
    UUID id;
    String quote;

    /**
     * What an interpreted string spells, and the literal as written, which is printed
     * for as long as the annotations still spell that value.
     */
    @Nullable
    String value;

    @Nullable
    String valueSource;

    @Override
    public void rpcSend(StructTagQuote after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, StructTagQuote::getQuote);
        q.getAndSend(after, StructTagQuote::getValue);
        q.getAndSend(after, StructTagQuote::getValueSource);
    }

    @Override
    public StructTagQuote rpcReceive(StructTagQuote before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withQuote(q.receive(before.getQuote()))
                .withValue(q.receive(before.getValue()))
                .withValueSource(q.receive(before.getValueSource()));
    }
}
