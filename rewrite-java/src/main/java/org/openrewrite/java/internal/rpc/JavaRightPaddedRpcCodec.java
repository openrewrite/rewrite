package org.openrewrite.java.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaRightPaddedRpcCodec extends DynamicDispatchRpcCodec<JRightPadded> {
    private final JavaSender sender = new JavaSender();
    private final JavaReceiver receiver = new JavaReceiver();

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
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
