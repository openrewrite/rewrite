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
package org.openrewrite.javascript.internal.rpc;

import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpcFactory;

/**
 * Manages the lifecycle of JavaScriptRewriteRpc instances, allowing for a single
 * RPC server per thread.
 */
public class JavaScriptRewriteRpcManager {
    private static final JavaScriptRewriteRpcManager INSTANCE = new JavaScriptRewriteRpcManager();

    private final ThreadLocal<JavaScriptRewriteRpc> rpc = new ThreadLocal<>();
    private final ThreadLocal<JavaScriptRewriteRpcFactory> factory = ThreadLocal.withInitial(() ->
            JavaScriptRewriteRpcFactory.DEFAULT);

    public static JavaScriptRewriteRpc getOrStart() {
        JavaScriptRewriteRpcManager manager = INSTANCE;
        JavaScriptRewriteRpc current = manager.rpc.get();
        //noinspection ConstantValue
        if (current == null) {
            current = manager.factory.get().create();
            manager.rpc.set(current);
        }
        return current;
    }

    public static void setFactory(JavaScriptRewriteRpcFactory factory) {
        INSTANCE.factory.set(factory);
    }

    public static void shutdown() {
        JavaScriptRewriteRpcManager manager = INSTANCE;
        JavaScriptRewriteRpc current = manager.rpc.get();
        //noinspection ConstantValue
        if (current != null) {
            current.shutdown();
            manager.rpc.remove();
        }
    }
}
