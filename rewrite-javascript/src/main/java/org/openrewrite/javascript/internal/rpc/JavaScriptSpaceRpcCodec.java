package org.openrewrite.javascript.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
public class JavaScriptSpaceRpcCodec extends DynamicDispatchRpcCodec<Space> {
    private final JavaScriptSender sender = new JavaScriptSender();
    private final JavaScriptReceiver receiver = new JavaScriptReceiver();

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
