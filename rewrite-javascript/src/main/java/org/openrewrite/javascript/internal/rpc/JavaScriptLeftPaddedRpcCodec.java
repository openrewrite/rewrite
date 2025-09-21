package org.openrewrite.javascript.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaScriptLeftPaddedRpcCodec extends DynamicDispatchRpcCodec<JLeftPadded> {
    private final JavaScriptSender sender = new JavaScriptSender();
    private final JavaScriptReceiver receiver = new JavaScriptReceiver();

    @Override
    public String getSourceFileType() {
        return JS.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends JLeftPadded> getType() {
        return JLeftPadded.class;
    }

    @Override
    public void rpcSend(JLeftPadded after, RpcSendQueue q) {
        //noinspection unchecked
        sender.visitLeftPadded(after, q);
    }

    @Override
    public JLeftPadded rpcReceive(JLeftPadded before, RpcReceiveQueue q) {
        //noinspection unchecked
        return receiver.visitLeftPadded(before, q);
    }
}
