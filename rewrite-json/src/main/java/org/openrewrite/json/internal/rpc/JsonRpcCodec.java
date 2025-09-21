package org.openrewrite.json.internal.rpc;

import org.openrewrite.json.tree.Json;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JsonRpcCodec extends DynamicDispatchRpcCodec<Json> {
    private final JsonSender sender = new JsonSender();
    private final JsonReceiver receiver = new JsonReceiver();

    @Override
    public String getSourceFileType() {
        return Json.Document.class.getName();
    }

    @Override
    public Class<? extends Json> getType() {
        return Json.class;
    }

    @Override
    public void rpcSend(Json after, RpcSendQueue q) {
        sender.visit(after, q);
    }

    @Override
    public Json rpcReceive(Json before, RpcReceiveQueue q) {
        return receiver.visitNonNull(before, q);
    }
}
