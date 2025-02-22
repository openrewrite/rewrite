package org.openrewrite.rpc.request;

import lombok.Value;

@Value
public class Generate implements RpcRequest {
    String id;
}
