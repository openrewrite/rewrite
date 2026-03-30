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
package org.openrewrite.javascript.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
public class JavaScriptSpaceRpcCodec extends DynamicDispatchRpcCodec<Space> {

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
        new JavaScriptSender().visitSpace(after, q);
    }

    @Override
    public Space rpcReceive(Space before, RpcReceiveQueue q) {
        return new JavaScriptReceiver().visitSpace(before, q);
    }
}
