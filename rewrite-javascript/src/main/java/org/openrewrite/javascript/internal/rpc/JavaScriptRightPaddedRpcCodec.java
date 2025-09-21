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
    private final JavaScriptSender sender = new JavaScriptSender();
    private final JavaScriptReceiver receiver = new JavaScriptReceiver();

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
        sender.visitRightPadded(after, q);
    }

    @Override
    public JRightPadded rpcReceive(JRightPadded before, RpcReceiveQueue q) {
        //noinspection unchecked
        return receiver.visitRightPadded(before, q);
    }
}
