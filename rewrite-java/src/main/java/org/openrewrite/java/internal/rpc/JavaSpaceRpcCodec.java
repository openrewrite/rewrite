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
package org.openrewrite.java.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
public class JavaSpaceRpcCodec extends DynamicDispatchRpcCodec<Space> {
    private final JavaSender sender = new JavaSender();
    private final JavaReceiver receiver = new JavaReceiver();

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends Space> getType() {
        return Space.class;
    }

    @Override
    public void rpcSend(Space after, RpcSendQueue q) {
        sender.visitSpace(after, q);
    }

    @Override
    public Space rpcReceive(Space before, RpcReceiveQueue q) {
        return receiver.visitSpace(before, q);
    }
}
