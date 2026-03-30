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
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaScriptRightPaddedRpcCodec extends DynamicDispatchRpcCodec<JRightPadded> {

    @Override
    public String getSourceFileType() {
        return JS.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends JRightPadded> getType() {
        return JRightPadded.class;
    }

    @Override
    public void rpcSend(JRightPadded after, RpcSendQueue q) {
        //noinspection unchecked
        new JavaScriptSender().visitRightPadded(after, q);
    }

    @Override
    public JRightPadded rpcReceive(JRightPadded before, RpcReceiveQueue q) {
        //noinspection unchecked
        return new JavaScriptReceiver().visitRightPadded(before, q);
    }
}
