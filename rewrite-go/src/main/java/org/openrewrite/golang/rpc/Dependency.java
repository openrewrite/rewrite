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
import org.openrewrite.rpc.request.RpcRequest;

/**
 * One dependency whose public API to enumerate, named by its Go module coordinate. A {@code null}
 * version names a standard-library package instead ({@code modulePath} is then an import path
 * like {@code net/http}).
 */
@Value
public class Dependency implements RpcRequest {
    String modulePath;

    @Nullable
    String version;
}
