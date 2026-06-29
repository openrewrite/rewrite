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
package org.openrewrite.python.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker indicating that a {@code J.Try.Catch} was written using the Python 2
 * {@code except E, e:} comma form rather than the Python 3 {@code except E as e:}
 * form.
 * <p>
 * The default printer renders the catch as {@code except E as e:}; when this
 * marker is present it emits the Py2 comma-based syntax instead so Python 2
 * sources round-trip byte-for-byte.
 */
@Value
@With
public class TupleExceptClause implements Marker, RpcCodec<TupleExceptClause> {
    UUID id;

    @Override
    public void rpcSend(TupleExceptClause after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public TupleExceptClause rpcReceive(TupleExceptClause before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
