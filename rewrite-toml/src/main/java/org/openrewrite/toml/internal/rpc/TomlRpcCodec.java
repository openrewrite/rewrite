/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml.internal.rpc;

import org.openrewrite.toml.tree.Toml;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class TomlRpcCodec extends DynamicDispatchRpcCodec<Toml> {
    private final TomlSender sender = new TomlSender();
    private final TomlReceiver receiver = new TomlReceiver();

    @Override
    public String getSourceFileType() {
        return Toml.Document.class.getName();
    }

    @Override
    public Class<? extends Toml> getType() {
        return Toml.class;
    }

    @Override
    public void rpcSend(Toml after, RpcSendQueue q) {
        sender.visit(after, q);
    }

    @Override
    public Toml rpcReceive(Toml before, RpcReceiveQueue q) {
        return receiver.visitNonNull(before, q);
    }
}
