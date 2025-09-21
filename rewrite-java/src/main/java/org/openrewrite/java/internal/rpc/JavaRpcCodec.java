package org.openrewrite.java.internal.rpc;

import org.openrewrite.java.tree.J;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JavaRpcCodec extends DynamicDispatchRpcCodec<J> {
    private final JavaSender sender = new JavaSender();
    private final JavaReceiver receiver = new JavaReceiver();

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
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
