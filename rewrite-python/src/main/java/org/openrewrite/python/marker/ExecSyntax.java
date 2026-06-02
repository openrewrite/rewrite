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
package org.openrewrite.python.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker indicating that a {@code J.MethodInvocation} represents a Python 2 exec statement
 * rather than a Python 3 exec function call.
 * <p>
 * In Python 2, exec is a statement with optional scope arguments:
 * <pre>
 * exec code                    # simple form
 * exec code in globals         # with globals dict
 * exec code in globals, locals # with globals and locals dicts
 * </pre>
 * <p>
 * The marker allows the printer to output the correct syntax (using {@code in} keyword
 * instead of function arguments) and allows recipes to distinguish between exec statements
 * and exec function calls.
 */
@Value
@With
public class ExecSyntax implements Marker, RpcCodec<ExecSyntax> {
    UUID id;

    @Override
    public void rpcSend(ExecSyntax after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
    }

    @Override
    public ExecSyntax rpcReceive(ExecSyntax before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
    }
}
