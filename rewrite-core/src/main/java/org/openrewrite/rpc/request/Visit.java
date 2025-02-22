/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc.request;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Value
public class Visit implements RpcRequest {
    String visitor;

    Map<String, Object> visitorOptions;

    String treeId;

    /**
     * An ID of the p value stored in the caller's local object cache.
     */
    String p;

    /**
     * A list of IDs representing the cursor whose objects are stored in the
     * caller's local object cache.
     */
    @Nullable
    List<String> cursor;
}
