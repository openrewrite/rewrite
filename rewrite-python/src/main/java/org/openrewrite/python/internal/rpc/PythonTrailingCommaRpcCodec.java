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
package org.openrewrite.python.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.marker.Marker;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Python-specific codec for TrailingComma that uses PythonReceiver.visitSpace
 * to properly handle PyComment in the suffix.
 */
@Getter
public class PythonTrailingCommaRpcCodec extends DynamicDispatchRpcCodec<TrailingComma> {

    @Override
    public String getSourceFileType() {
        return Py.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends TrailingComma> getType() {
        return TrailingComma.class;
    }

    @Override
    public void rpcSend(TrailingComma after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, TrailingComma::getSuffix, space -> new PythonSender().visitSpace(space, q));
    }

    @Override
    public TrailingComma rpcReceive(TrailingComma before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withSuffix(q.receive(before.getSuffix(), space -> new PythonReceiver().visitSpace(space, q)));
    }
}
