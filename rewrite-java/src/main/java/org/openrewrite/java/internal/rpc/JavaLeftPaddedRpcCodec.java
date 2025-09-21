package org.openrewrite.java.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaLeftPaddedRpcCodec extends DynamicDispatchRpcCodec<JLeftPadded> {
    private final JavaSender sender = new JavaSender();
    private final JavaReceiver receiver = new JavaReceiver();

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
        sender.visitLeftPadded(after, q);
    }

    @Override
    public JLeftPadded rpcReceive(JLeftPadded before, RpcReceiveQueue q) {
        //noinspection unchecked
        return receiver.visitLeftPadded(before, q);
    }
}
