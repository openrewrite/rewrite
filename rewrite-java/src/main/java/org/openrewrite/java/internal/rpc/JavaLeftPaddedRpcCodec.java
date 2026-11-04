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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@SuppressWarnings("rawtypes")
public class JavaLeftPaddedRpcCodec extends DynamicDispatchRpcCodec<JLeftPadded> {

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends JLeftPadded> getType() {
        return JLeftPadded.class;
    }

    @Override
    public void rpcSend(JLeftPadded after, RpcSendQueue q) {
        //noinspection unchecked
        new JavaSender().visitLeftPadded(after, q);
    }

    @Override
    public JLeftPadded rpcReceive(JLeftPadded before, RpcReceiveQueue q) {
        //noinspection unchecked
        return new JavaReceiver().visitLeftPadded(before, q);
    }
}
