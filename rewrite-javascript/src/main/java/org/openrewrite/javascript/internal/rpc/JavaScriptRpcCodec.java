package org.openrewrite.javascript.internal.rpc;

import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JavaScriptRpcCodec extends DynamicDispatchRpcCodec<J> {
    private final JavaScriptSender sender = new JavaScriptSender();
    private final JavaScriptReceiver receiver = new JavaScriptReceiver();

    @Override
    public String getSourceFileType() {
        return JS.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends J> getType() {
        return J.class;
    }

    @Override
    public void rpcSend(J after, RpcSendQueue q) {
        sender.visit(after, q);
    }

    @Override
    public J rpcReceive(J before, RpcReceiveQueue q) {
        return receiver.visitNonNull(before, q);
    }
}
