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
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.java.tree.J.ForLoop.Control} whose init and update clauses are absent in the
 * source — Go's condition-only {@code for cond {}} and infinite {@code for {}} loops. The parser fills the init
 * and update slots with {@link org.openrewrite.java.tree.J.Empty} placeholders so the control honours the same
 * contract every other parser produces (init and update are single-element lists safe to index at 0); this marker
 * records that those placeholders are synthetic so the Go printer omits them and their {@code ;} separators.
 */
@Value
@With
public class ImplicitForClauses implements Marker, RpcCodec<ImplicitForClauses> {
    UUID id;

    @Override
    public void rpcSend(ImplicitForClauses after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public ImplicitForClauses rpcReceive(ImplicitForClauses before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
