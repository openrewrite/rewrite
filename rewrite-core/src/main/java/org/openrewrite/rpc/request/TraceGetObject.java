package org.openrewrite.rpc.request;

import lombok.Value;

@Value
public class TraceGetObject implements RpcRequest {
    boolean receive;
    boolean send;
}
