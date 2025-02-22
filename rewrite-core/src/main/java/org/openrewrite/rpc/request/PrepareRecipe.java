package org.openrewrite.rpc.request;

import lombok.Value;

import java.util.Map;

@Value
public class PrepareRecipe implements RpcRequest {
    String id;
    Map<String, Object> options;
}
