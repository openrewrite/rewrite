package org.openrewrite.javascript.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

@Value
class InstallRecipesByPackage implements RpcRequest {
    Package recipes;

    @Value
    public static class Package {
        String packageName;

        @Nullable
        String version;
    }
}
