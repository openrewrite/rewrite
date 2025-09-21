package org.openrewrite.java.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaContainerRpcCodec extends DynamicDispatchRpcCodec<JContainer> {
    private final JavaSender sender = new JavaSender();
    private final JavaReceiver receiver = new JavaReceiver();

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends JContainer> getType() {
        return JContainer.class;
    }

    @Override
    public void rpcSend(JContainer after, RpcSendQueue q) {
        //noinspection unchecked
        sender.visitContainer(after, q);
    }

    @Override
    public JContainer rpcReceive(JContainer before, RpcReceiveQueue q) {
        //noinspection unchecked
        return receiver.visitContainer(before, q);
    }
}
