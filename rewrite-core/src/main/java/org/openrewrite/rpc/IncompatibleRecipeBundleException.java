/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moderne.jsonrpc.JsonRpcError;
import io.moderne.jsonrpc.JsonRpcException;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ObjectMappers;

import java.io.IOException;

/**
 * Thrown by {@link RewriteRpc} when a language server refuses to install a recipe bundle whose declared range
 * for the server's own OpenRewrite package excludes the server's version.
 */
@Getter
public class IncompatibleRecipeBundleException extends RuntimeException {
    /**
     * The JSON-RPC error code of a refusal, outside the range JSON-RPC reserves (-32768 to -32000).
     * Its data is a JSON string holding this exception's four fields.
     */
    private static final int CODE = 1001;

    private static final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);

    private final String packageName;
    private final String packageVersion;
    private final String requiredRange;
    private final String hostVersion;

    private IncompatibleRecipeBundleException(String message, JsonNode data) {
        super(message);
        this.packageName = data.path("packageName").asText();
        this.packageVersion = data.path("packageVersion").asText();
        this.requiredRange = data.path("requiredRange").asText();
        this.hostVersion = data.path("hostVersion").asText();
    }

    /**
     * The refusal a failed request carries, or null when it failed for another reason.
     */
    static @Nullable IncompatibleRecipeBundleException from(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof JsonRpcException) {
                JsonRpcError.Detail detail = ((JsonRpcException) t).getError().getError();
                if (detail.getCode() != CODE || detail.getData() == null) {
                    return null;
                }
                try {
                    return new IncompatibleRecipeBundleException(detail.getMessage(), mapper.readTree(detail.getData()));
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
