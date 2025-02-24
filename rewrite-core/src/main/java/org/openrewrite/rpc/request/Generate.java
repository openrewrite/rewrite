package org.openrewrite.rpc.request;

import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.Value;

import static java.util.Collections.emptyList;

@Value
public class Generate implements RpcRequest {
    String id;

    public static class Handler extends JsonRpcMethod<Generate> {
        @Override
        protected Object handle(Generate request) throws Exception {
            // TODO implement me!
            return emptyList();
        }
    }
}
