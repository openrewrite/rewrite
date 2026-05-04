/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.golang.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.Parse;
import org.openrewrite.rpc.request.RpcRequest;

import java.util.List;

/**
 * Go-specific Parse RPC payload. Mirrors the rewrite-core {@code Parse}
 * shape but adds optional {@code module} and {@code goModContent} fields
 * the Go server uses to build a project-aware {@code ProjectImporter}
 * for type attribution. Other languages don't need these fields and the
 * Go server ignores them when absent.
 */
@Value
public class GoParseRequest implements RpcRequest {
    List<Parse.Input> inputs;

    @Nullable
    String relativeTo;

    /**
     * Module path declared by the project's go.mod, e.g. {@code example.com/foo}.
     * When set, the Go server constructs a ProjectImporter and uses it for
     * type attribution; the inputs in this batch are treated as siblings
     * of that module.
     */
    @Nullable
    String module;

    /**
     * Raw go.mod content. The Go server parses it for {@code require}
     * directives, registering each as a known module path so imports of
     * those modules resolve to stub {@code *types.Package} objects.
     */
    @Nullable
    String goModContent;
}
