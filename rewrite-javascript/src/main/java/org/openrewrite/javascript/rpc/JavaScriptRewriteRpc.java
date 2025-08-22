/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.javascript.internal.rpc.JavaScriptRewriteRpcManager;
import org.openrewrite.javascript.internal.rpc.JavaScriptRewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpc;

import java.io.File;
import java.time.Duration;

/**
 * A client for communicating with a JavaScript {@link RewriteRpc} server.
 */
public class JavaScriptRewriteRpc extends RewriteRpc {
    private final JavaScriptRewriteRpcProcess process;

    JavaScriptRewriteRpc(JavaScriptRewriteRpcProcess process, Environment marketplace, Duration timeout) {
        super(process.getRpcClient(), marketplace, timeout);
        this.process = process;
    }

    public static JavaScriptRewriteRpc getOrStart() {
        return JavaScriptRewriteRpcManager.getOrStart();
    }

    public static void setFactory(JavaScriptRewriteRpcFactory factory) {
        JavaScriptRewriteRpcManager.setFactory(factory);
    }

    public static void shutdownCurrent() {
        JavaScriptRewriteRpcManager.shutdown();
    }

    public void shutdown() {
        process.interrupt();
        try {
            process.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }

    public int installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    public int installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }
}
