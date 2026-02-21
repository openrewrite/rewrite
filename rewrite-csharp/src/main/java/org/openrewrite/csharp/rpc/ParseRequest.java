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
package org.openrewrite.csharp.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

import java.util.List;

/**
 * RPC request to parse C# source files.
 */
@Value
class ParseRequest implements RpcRequest {
    /**
     * Input specifications for files to parse.
     */
    List<Input> inputs;

    /**
     * Assembly references for type attribution. Each entry is either:
     * <ul>
     *   <li>A NuGet package name (e.g., "Newtonsoft.Json")</li>
     *   <li>A NuGet package name with version (e.g., "Newtonsoft.Json@13.0.1")</li>
     *   <li>A direct path to a DLL file</li>
     * </ul>
     * When null, parsing proceeds without type attribution.
     */
    @Nullable List<String> assemblyReferences;

    @Value
    static class Input {
        /**
         * Path to the source file.
         */
        String sourcePath;

        /**
         * Optional text content. If provided, the file is parsed from this text
         * instead of reading from the file system.
         */
        @Nullable String text;
    }
}
