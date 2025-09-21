package org.openrewrite.javascript.internal.rpc;

import lombok.Getter;
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Getter
@SuppressWarnings("rawtypes")
public class JavaScriptContainerRpcCodec extends DynamicDispatchRpcCodec<JContainer> {
    private final JavaScriptSender sender = new JavaScriptSender();
    private final JavaScriptReceiver receiver = new JavaScriptReceiver();

    @Override
    public String getSourceFileType() {
        return JS.CompilationUnit.class.getName();
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
