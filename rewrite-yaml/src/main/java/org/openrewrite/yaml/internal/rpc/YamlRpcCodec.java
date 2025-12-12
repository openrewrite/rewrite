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
package org.openrewrite.yaml.internal.rpc;

import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.yaml.tree.Yaml;

public class YamlRpcCodec extends DynamicDispatchRpcCodec<Yaml> {
    private final YamlSender sender = new YamlSender();
    private final YamlReceiver receiver = new YamlReceiver();

    @Override
    public String getSourceFileType() {
        return Yaml.Documents.class.getName();
    }

    @Override
    public Class<? extends Yaml> getType() {
        return Yaml.class;
    }

    @Override
    public void rpcSend(Yaml after, RpcSendQueue q) {
        sender.visit(after, q);
    }

    @Override
    public Yaml rpcReceive(Yaml before, RpcReceiveQueue q) {
        return receiver.visitNonNull(before, q);
    }
}
