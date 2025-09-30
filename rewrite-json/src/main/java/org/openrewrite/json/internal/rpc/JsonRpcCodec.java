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
package org.openrewrite.json.internal.rpc;

import org.openrewrite.json.tree.Json;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JsonRpcCodec extends DynamicDispatchRpcCodec<Json> {
    private final JsonSender sender = new JsonSender();
    private final JsonReceiver receiver = new JsonReceiver();

    @Override
    public String getSourceFileType() {
        return Json.Document.class.getName();
    }

    @Override
    public Class<? extends Json> getType() {
        return Json.class;
    }

    @Override
    public void rpcSend(Json after, RpcSendQueue q) {
        sender.visit(after, q);
    }

    @Override
    public Json rpcReceive(Json before, RpcReceiveQueue q) {
        return receiver.visitNonNull(before, q);
    }
}
