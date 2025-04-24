package org.openrewrite.javascript.rpc;

import lombok.Value;
import org.openrewrite.rpc.request.RpcRequest;

import java.io.File;
import java.nio.file.Path;

@Value
class InstallRecipesByFile implements RpcRequest {
    File recipes;
}
