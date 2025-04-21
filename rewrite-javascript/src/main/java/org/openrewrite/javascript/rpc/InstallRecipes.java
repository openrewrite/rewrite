package org.openrewrite.javascript.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

@Value
class InstallRecipes implements RpcRequest {
    String packageName;

    @Nullable
    String version;
}
